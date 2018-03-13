package top.mxonline.upload;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileUploadBase;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;

@WebServlet("/upload")
public class UploadServlet extends HttpServlet {

	/**
	 * this is the default serial version.
	 */
	
	private static final String ERR_CODE_1 = "0x001";
	private static final String ERR_CODE_2 = "0x002";
	private static final long serialVersionUID = 1L;
	private static final Logger logger = Logger.getLogger("UploadServlet");
	private static final String PROPERTY_PATH = "C:/property/uploader.properties";
	private String savePath;
	private String fileName;
	private String tmpPath;
	
	public UploadServlet() {
		loadProperties();
	}
	
	@Override
    protected void doOptions(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
	    resp.setHeader("Access-Control-Allow-Credentials", "false");
        resp.setHeader("Access-Control-Allow-Origin", "*");
        resp.setHeader("Access-Control-Allow-Methods", "*");
        resp.setHeader("Access-Control-Allow-Headers", "x-requested-with,content-type");
        resp.setContentType("application/json");
        resp.setCharacterEncoding("utf-8");
        doPost(req,resp);
        super.doOptions(req, resp);
    }

    @Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.setHeader("Access-Control-Allow-Origin", "*");
		resp.setCharacterEncoding("utf-8");
		resp.setHeader("Access-Control-Allow-Methods", "*");
	    resp.setContentType("text/html; charset=utf-8");  
		PrintWriter pw = resp.getWriter();
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy_MM_dd_HH_mm");
		DiskFileItemFactory factory = new DiskFileItemFactory();
		factory.setSizeThreshold(4096);//set the buffer size
		try {
			File save = new File(savePath);
			File tmp = new File(tmpPath);
			if(!save.exists()){
				save.mkdirs();
			}
			if(!tmp.exists()) {
				tmp.mkdirs();
			}
			factory.setRepository(new File(tmpPath));//set the file buffer directory
			ServletFileUpload fileUpload = new ServletFileUpload(factory);
			List<FileItem> items = fileUpload.parseRequest(req);
			Iterator<FileItem> i = items.iterator();
			while(i.hasNext()) {
				FileItem fi = (FileItem) i.next();
				String originName = fi.getName();
				if (originName != null) {					
					File savedFile = new File(savePath, fileName+ originName.substring(originName.indexOf('.')));
					if(savedFile.exists()) {
						savedFile.renameTo(new File(savedFile.getAbsolutePath() + ".keep" + sdf.format(System.currentTimeMillis())));		
					}
					
					fi.write(savedFile);
					
				}
			}
			pw.write("上传成功");
			pw.flush();					
		} catch(FileUploadBase.FileSizeLimitExceededException e) {
			pw.write("上传失败，文件大小超过限制");
			pw.flush();
			logger.log(Level.SEVERE, "File Size Limit Exceeded:" + e.getMessage());
			e.printStackTrace();
		} 
		catch (FileUploadException e) {
			pw.write("上传失败，错误代码：" + ERR_CODE_1);
			pw.flush();
			logger.log(Level.SEVERE, ERR_CODE_1+ ":" + e.getMessage());
			e.printStackTrace();
			
		} catch (Exception e) {		
			pw.write("上传失败，错误代码：" + ERR_CODE_2);
			pw.flush();
			logger.log(Level.SEVERE, ERR_CODE_2+ ":" + e.getMessage());
			e.printStackTrace();
		}
	}

	private void loadProperties() {
		Properties prop = new Properties();     
        try{
            //读取属性文件properties
            InputStream in = new BufferedInputStream (new FileInputStream(PROPERTY_PATH));
            prop.load(in);     ///加载属性列表
            savePath = prop.getProperty("SAVE_PATH");
            tmpPath = prop.getProperty("TEMP_PATH");
            fileName = prop.getProperty("FILE_NAME");
            logger.info(savePath);
            logger.info(tmpPath);
            logger.info(fileName);
            in.close();
        }
        catch(Exception e){
        	logger.log(Level.SEVERE, "Property Not Found:" + e.getMessage());
        	e.printStackTrace();
        }
    }	
}
