package top.mxonline.upload;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.HttpSolrClient;

import top.mxonline.utils.ExcelReader;

@WebServlet("/update")
public class UpdateDataServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;
    private static final String ERR_CODE1 = "0x001";
	private static final String ERR_CODE2 = "0x002";
	private static final String ERR_CODE3 = "0x003";
	private static final String PROPERTY_PATH = "C:/property/update.properties";
	private static final Logger logger = Logger.getLogger(UpdateDataServlet.class.getName());
	private String formatSourcePath;
	private String formatSourceFileName;
	private String formatResultPath;
	private String formatResultFileName;
	private String indexSourceFile;
	
	private String solrServerUrl;
	private String solrCore;

	public UpdateDataServlet() {
		loadProperties();
	}

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
	    resp.setHeader("Access-Control-Allow-Origin", "*");
		resp.setCharacterEncoding("utf-8");
		resp.setContentType("text/html; charset=utf-8");
		PrintWriter pw = resp.getWriter();
		try {
		boolean success = ExcelReader.convertToCsv(formatSourcePath +"/" + formatSourceFileName, formatResultPath + "/" + formatResultFileName);

		if (success) {
			SolrClient  solrClient = getSolrClient(solrServerUrl, solrCore);
			solrClient.deleteByQuery("*:*");
			URL indexingUrl = new URL(solrServerUrl + "/" +solrCore + "/update/csv?commit=true&stream.file=" + indexSourceFile + "&stream.contentType=text/plain;charset=UTF-8");
			HttpURLConnection urlConnection = (HttpURLConnection) indexingUrl.openConnection();
			urlConnection.connect();
			InputStream is = urlConnection.getInputStream();
			BufferedReader buffer = new BufferedReader(new InputStreamReader(is));
			StringBuffer bs = new StringBuffer();
	        String l = null;
	        while((l= buffer.readLine())!=null){
	            bs.append(l);
	        }
	        if (bs.toString().contains("Exception")){
	        	pw.write("建立索引失败，错误代码：" + ERR_CODE1);
	        	logger.log(Level.SEVERE, "建立索引失败，错误代码：" + ERR_CODE1 +",SOLR内部错误，详细错误信息：" + bs.toString());
	            pw.flush();	
	        } else {
	        	pw.write("一键更新成功");
	    		pw.flush();	
	        }
	        
			
		} else {
			pw.write("格式化失败，错误代码：" + ERR_CODE1);
			logger.log(Level.SEVERE,"格式化失败，错误代码：" + ERR_CODE1 + ",XLSX格式转换时发生错误");
			pw.flush();
		}

		} catch (SolrServerException e) {
			pw.write("清除索引失败，错误代码：" + ERR_CODE2);
			logger.log(Level.SEVERE,"格式化失败，错误代码：" + ERR_CODE2 + ",SOLR服务器内部错误，详细错误信息" + e.getMessage());
			pw.flush();
			e.printStackTrace();
		}catch (Exception e) {
			pw.write("更新失败，错误代码：" + ERR_CODE3 );
			logger.log(Level.SEVERE,"格式化失败，错误代码：" + ERR_CODE3 + ",详细错误信息" + e.getMessage());
			e.printStackTrace();
		} 

	}

	private void loadProperties() {
		Properties prop = new Properties();
		try {
			InputStream in = new BufferedInputStream(new FileInputStream(PROPERTY_PATH));
			prop.load(in);
			formatSourcePath = prop.getProperty("SOURCE_PATH");
			formatSourceFileName = prop.getProperty("SOURCE_FILE_NAME");
			formatResultPath = prop.getProperty("RESULT_PATH");
			formatResultFileName = prop.getProperty("RESULT_FILE_NAME");
			solrServerUrl = prop.getProperty("SOLR_SERVER");
	        solrCore = prop.getProperty("SOLR_CORE");
	        indexSourceFile = prop.getProperty("SOURCE_FILE");

		} catch (FileNotFoundException e) {
			logger.log(Level.SEVERE, "无法找到配置文件，详细错误信息：" + e.getMessage());
			e.printStackTrace();
		} catch (IOException e) {
			logger.log(Level.SEVERE, "读取属性文件时发生错误," + e.getMessage());
			e.printStackTrace();
		}
	}

	private SolrClient getSolrClient(String solrServerUrl, String solrCore) {		
		return new HttpSolrClient(solrServerUrl + "/" + solrCore);
	}
	
}
