package top.mxonline.utils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.poi.hssf.usermodel.HSSFDateUtil;
import org.apache.poi.openxml4j.opc.OPCPackage;
import org.apache.poi.xssf.eventusermodel.XSSFReader;
import org.apache.poi.xssf.model.SharedStringsTable;
import org.apache.poi.xssf.usermodel.XSSFRichTextString;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;
import org.xml.sax.helpers.XMLReaderFactory;

/**
 * Excel超大数据读取，抽象Excel2007读取器，excel2007的底层数据结构是xml文件，采用SAX的事件驱动的方法解析
 * xml，需要继承DefaultHandler，在遇到文件内容时，事件会触发，这种做法可以大大降低 内存的耗费，特别使用于大数据量的文件。
 *
 * @version 2018-3-16
 */
public abstract class ExcelReader extends DefaultHandler {

    private static final Logger logger = Logger.getLogger(ExcelReader.class.getName());
    // 共享字符串表
    private SharedStringsTable sst;

    // 上一次的内容
    private String lastContents;
    private boolean nextIsString;

    private int sheetIndex = -1;
    private List<String> rowList = new ArrayList<String>();
    // 当前行
    private int curRow = 0;
    // 当前列
    private int curCol = 0;
    // 日期标志
    private boolean dateFlag;
    // 数字标志
    private boolean numberFlag;

    private boolean isTElement;

    private static int headerSize;

    /**
     * 遍历工作簿中所有的电子表格
     *
     * @param filename
     * @throws Exception
     */
    public void process(String filename) throws Exception {
        OPCPackage pkg = OPCPackage.open(filename);
        XSSFReader r = new XSSFReader(pkg);
        SharedStringsTable sst = r.getSharedStringsTable();
        XMLReader parser = fetchSheetParser(sst);
        Iterator<InputStream> sheets = r.getSheetsData();
        while (sheets.hasNext()) {
            curRow = 0;
            sheetIndex++;
            InputStream sheet = sheets.next();
            InputSource sheetSource = new InputSource(sheet);
            parser.parse(sheetSource);
            sheet.close();
        }
    }

    /**
     * 只遍历一个电子表格，其中sheetId为要遍历的sheet索引，从1开始，1-3
     *
     * @param filename
     * @param sheetId
     * @throws Exception
     */
    public void process(String filename, int sheetId) throws Exception {
        OPCPackage pkg = OPCPackage.open(filename);
        XSSFReader r = new XSSFReader(pkg);
        SharedStringsTable sst = r.getSharedStringsTable();
        XMLReader parser = fetchSheetParser(sst);
        // 根据 rId# 或 rSheet# 查找sheet
        InputStream sheet2 = r.getSheet("rId" + sheetId);
        // sheetIndex++;
        InputSource sheetSource = new InputSource(sheet2);
        parser.parse(sheetSource);
        sheet2.close();
    }

    public XMLReader fetchSheetParser(SharedStringsTable sst) throws SAXException {
        XMLReader parser = XMLReaderFactory.createXMLReader("org.apache.xerces.parsers.SAXParser");
        this.sst = sst;
        parser.setContentHandler(this);
        return parser;
    }

    public void startElement(String uri, String localName, String name, Attributes attributes) throws SAXException {

        // System.out.println("startElement: " + localName + ", " + name + ", "
        // + attributes);

        // c => 单元格
        if ("c".equals(name)) {
            // 如果下一个元素是 SST 的索引，则将nextIsString标记为true
            String cellType = attributes.getValue("t");
            if ("s".equals(cellType)) {
                nextIsString = true;
            } else {
                nextIsString = false;
            }
            // 日期格式
            String cellDateType = attributes.getValue("s");
            if ("1".equals(cellDateType)) {
                dateFlag = true;
            } else {
                dateFlag = false;
            }
            String cellNumberType = attributes.getValue("s");
            if ("2".equals(cellNumberType)) {
                numberFlag = true;
            } else {
                numberFlag = false;
            }

        }
        // 当元素为t时
        if ("t".equals(name)) {
            isTElement = true;
        } else {
            isTElement = false;
        }

        // 置空
        lastContents = "";
    }

    public void endElement(String uri, String localName, String name) throws SAXException {

        // System.out.println("endElement: " + localName + ", " + name);

        // 根据SST的索引值的到单元格的真正要存储的字符串
        // 这时characters()方法可能会被调用多次
        if (nextIsString) {
            try {
                int idx = Integer.parseInt(lastContents);
                lastContents = new XSSFRichTextString(sst.getEntryAt(idx)).toString();
            } catch (Exception e) {
                //此处过多的log会影响服务器响应
                //logger.log(Level.SEVERE, "读取单元格失败，详细错误信息：" + e.getMessage());
                //e.printStackTrace();
            }
        }
        // t元素也包含字符串
        if (isTElement) {
            String value = lastContents.trim();
            rowList.add(curCol, value);
            curCol++;
            isTElement = false;
            // v => 单元格的值，如果单元格是字符串则v标签的值为该字符串在SST中的索引
            // 将单元格内容加入rowlist中，在这之前先去掉字符串前后的空白符
        } else if ("v".equals(name)) {
            String value = lastContents.trim();
            value = value.equals("") ? " " : value;
            try {
                // 日期格式处理
                if (dateFlag) {
                    Date date = HSSFDateUtil.getJavaDate(Double.valueOf(value));
                    SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy");
                    value = dateFormat.format(date);
                }
                // 数字类型处理
                if (numberFlag) {
                    BigDecimal bd = new BigDecimal(value);
                    value = bd.setScale(3, BigDecimal.ROUND_UP).toString();
                }
            } catch (Exception e) {
                // 转换失败仍用读出来的值，此处过多的log会影响服务器响应
                //logger.log(Level.SEVERE, "转换失败，详细错误信息：" + e.getMessage());
                //e.printStackTrace();
            }
            rowList.add(curCol, value);
            curCol++;
        } else {
            // 如果标签名称为 row ，这说明已到行尾，调用 optRows() 方法
            if (name.equals("row")) {
                getRows(sheetIndex + 1, curRow, rowList);
                rowList.clear();
                curRow++;
                curCol = 0;
            }
        }

    }

    public void characters(char[] ch, int start, int length) throws SAXException {
        // 得到单元格内容的值
        lastContents += new String(ch, start, length);
    }

    /**
     * 获取行数据回调
     *
     * @param sheetIndex
     * @param curRow
     * @param rowList
     */
    public abstract void getRows(int sheetIndex, int curRow, List<String> rowList);

    public int getHeaderSize(List<String> rowList) {
        return rowList.size();
    }

    public static boolean convertToCsv(String xlsx, String csv) {
        StringBuilder sb = new StringBuilder();
        try {
            File rst = new File(csv);
            if (!new File(rst.getParent()).exists()) {
                new File(rst.getParent()).mkdirs();
            }
            if (rst.exists()) {
                rst.delete();
            }
            OutputStreamWriter pw = new OutputStreamWriter(new FileOutputStream(csv), "UTF-8");
            ExcelReader reader = new ExcelReader() {
                public void getRows(int sheetIndex, int curRow, List<String> rowList) {
                    StringBuilder rowSb = new StringBuilder();
                    int count = 0;
                    //如果是第一行标题行，则静态初始化列数。
                    if (curRow == 0) {
                        headerSize = rowList.size();
                    }
                    //如果改行的列数与标题行一致（说明没有数据缺损），则格式化成csv；否则丢弃该行数据。
                    if (rowList.size() == headerSize) {
                        for (int i = 0; i < rowList.size(); i++) {
                            String str = rowList.get(i);
                            //为了防止原内容中本身包含\和|而导致csv格式以及转换后的json格式问题，将转义其为#
                            if(str.contains("\\")){
                                str = str.replaceAll("\\\\","#");
                                logger.log(Level.WARNING, "已将第" + curRow + "行，第" + i + "单元格中元数据中\\替换为#");
                            }
                            if(str.contains("|")){
                                str = str.replaceAll("\\|","#");
                                logger.log(Level.WARNING, "已将第" + curRow + "行，第" + i + "单元格中元数据中|替换为#");
                            }
                            if(str != null && !str.equalsIgnoreCase("#N/A") && !str.equalsIgnoreCase("")){
                                rowSb.append(str).append("|");
                                count ++;
                            } else {
                                logger.log(Level.WARNING, "已将第" + curRow + "行数据移除，因该行第" + i + "单元格中元数据中为空或#N/A，不符合要求。");
                                continue;
                            }

                        }
                        if(count >= rowList.size()) {
                            sb.append(rowSb).append("\n");
                        }
                    }
                }
            };
            reader.process(xlsx, 1);
            byte b[] = { (byte) 0xEF, (byte) 0xBB, (byte) 0xBF };
            // String bom = new String(b);
            // pw.write(bom);
            pw.write(sb.toString());

            pw.close();
        } catch (UnsupportedEncodingException | FileNotFoundException e) {
            logger.log(Level.SEVERE, "详细错误信息：" + e.getMessage());
            e.printStackTrace();
            return false;
        } catch (IOException e) {
            logger.log(Level.SEVERE, "详细错误信息：" + e.getMessage());
            e.printStackTrace();
            return false;
        } catch (Exception e) {
            logger.log(Level.SEVERE, "详细错误信息：" + e.getMessage());
            e.printStackTrace();
            return false;
        }
        return true;
    }

}