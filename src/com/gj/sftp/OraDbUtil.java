package com.gj.sftp;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Properties;

import org.apache.log4j.Logger;

import com.jcraft.jsch.SftpException;

import oracle.jdbc.OracleConnection;
import oracle.jdbc.OraclePreparedStatement;
import oracle.sql.CLOB;

public class OraDbUtil {
	
	private static Logger log = Logger.getLogger(OraDbUtil.class.getName());
	
	
    public static final String getLocalPath(){
		try{
			String path = GJSFTP.class.getResource("/").getPath();
			if(path == null || path.equals("")) throw new NullPointerException();
			return path;
		}catch(NullPointerException e){
			
		}
		
		String path = GJSFTP.class.getProtectionDomain().getCodeSource().getLocation().getPath();
		return new File(path).getParentFile().getParent();
	}
	
    
   
    
	
    private static final String DB_URL = "jdbc:oracle:thin:@127.0.0.1:1521:oracle";
	private static final String DB_USER = "QSG";
	private static final String DB_PASSWORD = "oracle";
	private static Connection conn = null;
	
//	private static String DB_URL = null;
//	private static String DB_USER = null;
//	private static String DB_PASSWORD = null;
//	private static Connection conn = null;

	
	//建立连接
	public static Connection getConnection() throws IOException {
		
//		Properties pro=new Properties();
//		InputStream in = GJSFTP.class.getClassLoader().getResourceAsStream("properties.properties");
//		pro.load(in);
//		DB_URL = pro.getProperty("OUT_VMI"); //从远程主机的路径下载
//		DB_USER = pro.getProperty("OUT_VMI");
//		DB_PASSWORD = pro.getProperty("OUT_VMI");
		try {
			if (conn == null) {
				Class.forName("oracle.jdbc.driver.OracleDriver");

				conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
				System.out.println("数据库连接成功！");
			}
		

			return conn;
		} catch (Exception e) {
			e.printStackTrace();
		}

		return null;
	}
	
	//获取xml的内容，
	public static String readFileContent(File file) {
	    BufferedReader reader = null;
	    StringBuffer sbf = new StringBuffer();
	    try {
	        reader = new BufferedReader(new FileReader(file));
	        String tempStr;
	        while ((tempStr = reader.readLine()) != null) {
	            sbf.append(tempStr);
	        }
	        reader.close();
	        return sbf.toString();
	    } catch (IOException e) {
	        e.printStackTrace();
	    } finally {
	        if (reader != null) {
	            try {
	                reader.close();
	            } catch (IOException e1) {
	                e1.printStackTrace();
	            }
	        }
	    }
	    return sbf.toString();
	}
	
    
	// 将字CLOB转成STRING类型
	public String ClobToString(CLOB clob) throws SQLException,IOException {

		String reString = "";
	
		Reader is = clob.getCharacterStream();// 得到流
	
		BufferedReader br = new BufferedReader(is);
	
		String s = br.readLine();
	
		StringBuffer sb = new StringBuffer();
	
		while (s != null) {// 执行循环将字符串全部取出付值给StringBuffer由StringBuffer转成STRING
	
			sb.append(s);
		
			s = br.readLine();
	
		}
	
		reString = sb.toString();
	
		return reString;

	}
	
	
	/**
	* 读clob字段
	*/
	@SuppressWarnings("unused")
	public void XmlOutEbs() throws IOException, SQLException {
		
    	Properties pro=new Properties();
		InputStream stream = OraDbUtil.class.getClassLoader().getResourceAsStream("properties.properties");
		pro.load(stream);
		String out_order =  pro.getProperty("OUT_ORDER"); //本地文件上传的路径
        String out_ack_vmi =  pro.getProperty("OUT_ACK_VMI"); //本地文件上传的路径
        String out_ack_ordexe =  pro.getProperty("OUT_ACK_ORDEXE"); //本地文件上传的路径

        Connection conn = null;
        String sql = null;
        PreparedStatement  stmt = null;
        ResultSet rs = null;
        
        conn = getConnection(); //建立连接
		
		sql = "SELECT XML_NAME, XML_CONTENT, REQUEST_TYPE FROM XML_OUT_DB WHERE XML_STATUS = 1 for update";
		
		stmt = conn.prepareStatement(sql);
		rs = stmt.executeQuery();        //执行查询语句
        
		try {
			
			
			while (rs.next()) { // 取出CLOB对象
				String filename = null;
				CLOB clob = null;
				String flag = null;
				String content = null;
				filename = rs.getString("XML_NAME"); //获取文件名
				clob = (CLOB) rs.getClob("XML_CONTENT"); //获取CLOB数据
				flag = rs.getString("REQUEST_TYPE"); //获取文件的需求类型

				content = ClobToString(clob); //将CLOB数据转成String
				
				if(flag.equals("VMI")) {
					BufferedWriter out1 = null;
					out1 = new BufferedWriter(new FileWriter(out_ack_vmi + filename));
					out1.write(content); //写入文件
					System.out.println("数据库成功导出xml文件："+filename+"，至目录：" + out_ack_vmi);
					out1.flush(); 
					
					//在数据读取之后，将数据行状态更新，避免重复读取
					String updatesql = "UPDATE XML_OUT_DB SET XML_STATUS = 2 where XML_NAME = '"+filename+"'";
					PreparedStatement stmt2 = conn.prepareStatement(updatesql);
					ResultSet rs2 = stmt2.executeQuery();        //执行查询语句
					try {
						out1.close();
					}catch(Exception ee) {
						ee.printStackTrace();
					}
					
				}
				else if(flag.equals("ORDEXE")) {
					BufferedWriter out1 = null;
					out1 = new BufferedWriter(new FileWriter(out_ack_ordexe + filename));
					out1.write(content); //写入文件
					System.out.println("数据库成功导出xml文件："+filename+"，至目录：" + out_ack_ordexe);
					out1.flush(); 
					
					//在数据读取之后，将数据行状态更新，避免重复读取
					String updatesql = "UPDATE XML_OUT_DB SET XML_STATUS = 2 where XML_NAME = '"+filename+"'";
					PreparedStatement stmt2 = conn.prepareStatement(updatesql);
					ResultSet rs2 = stmt2.executeQuery();        //执行查询语句
					try {
						out1.close();
					}catch(Exception ee) {
						ee.printStackTrace();
					}
					
				}
				else if(flag.equals("ORDER")) {
					BufferedWriter out1 = null;
					out1 = new BufferedWriter(new FileWriter(out_order + filename));
					out1.write(content); //写入文件
					System.out.println("数据库成功导出xml文件："+filename+"，至目录：" + out_order);
					out1.flush(); 
					
					//在数据读取之后，将数据行状态更新，避免重复读取
					String updatesql = "UPDATE XML_OUT_DB SET XML_STATUS = 2 where XML_NAME = '"+filename+"'";
					PreparedStatement stmt2 = conn.prepareStatement(updatesql);
					ResultSet rs2 = stmt2.executeQuery();        //执行查询语句
					try {
						out1.close();
					}catch(Exception ee) {
						ee.printStackTrace();
					}
					
				}
			}
			
		}catch(Exception e) {
			e.printStackTrace();
			System.out.println("数据库中没有可导出的数据"); 
		}
		finally {
			
	        stream.close();
		}
	}
	
	//单个文件导入数据库
	public boolean XmlInEbs(String backupPath, File file, String type) throws SQLException, IOException {

		
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		String filename = file.getName();
//		String inFile = localPath + file; //inFile为文件全路径
		String date = sdf.format(new Date(file.lastModified()));
		int status = 0;
	
		Connection conn = getConnection(); //建立连接
	 	
	 	conn.setAutoCommit(false);

		String sql = "INSERT INTO XML_IN_DB (XML_NAME, XML_CONTENT, CREATE_DATE, REQUEST_TYPE, XML_STATUS) values (?,?,?,?,?)";
		
		CLOB clob = new CLOB((OracleConnection)conn);
		clob = CLOB.createTemporary((OracleConnection)conn,true,1);
		clob.setString(1,readFileContent(file)); //获取文件的字符串转成CLOB
		OracleConnection OCon = (OracleConnection)conn;
        OraclePreparedStatement ps = (OraclePreparedStatement)OCon.prepareCall(sql);
	
        ps.setString(1, filename); //插入名称
        ps.setClob(2, clob); //插入CLOB数据
        ps.setString(3, date); //插入时间
        
        if(type.equals("VMI")) {
        	ps.setString(4, type); //插入请求类型    
		}
		else if(type.equals("ORDEXE")) {
			ps.setString(4, type); //插入请求类型
		}
		else if(type.equals("ORDER")) {
			ps.setString(4, type); //插入请求类型
		} 
        
        ps.setInt(5, status); //插入处理状态

        ps.executeUpdate();
        ps.close();
        ps = null;
        
        OCon.commit();
        conn.setAutoCommit(true);
        
        this.copyFile(file, backupPath);
        
        System.out.println("文件:" + file + "已导入数据库，并备份");

		return true;

	}
	
	
	public boolean XmlInEbs1(File file, String type) throws IOException {

		
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		String filename = file.getName();
		String date = sdf.format(new Date(file.lastModified()));
		int status = 0;
	
		Connection conn = null;
		String sql = null;
		CLOB clob = null;
	
	 	try {
	 		conn = getConnection(); //建立连接
			conn.setAutoCommit(false);
			sql = "INSERT INTO XML_IN_DB (XML_NAME, XML_CONTENT, CREATE_DATE, REQUEST_TYPE, XML_STATUS) values (?,?,?,?,?)";
			
			clob = new CLOB((OracleConnection)conn);
			clob = CLOB.createTemporary((OracleConnection)conn,true,1);
			clob.setString(1,readFileContent(file)); //获取文件的字符串转成CLOB
			OracleConnection OCon = (OracleConnection)conn;
	        OraclePreparedStatement ps = (OraclePreparedStatement)OCon.prepareCall(sql);
		
	        ps.setString(1, filename); //插入名称
	        ps.setClob(2, clob); //插入CLOB数据
	        ps.setString(3, date); //插入时间
	        ps.setString(4, type); //插入请求类型   
	        ps.setInt(5, status); //插入处理状态

	        ps.executeUpdate();
	        ps.close();
	        ps = null;
	        
	        OCon.commit();
	        conn.setAutoCommit(true);

			return true;
			
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return false;

	}
	
	
	
	
	
	
	
	
	
	
		
	//复制文件
//	  public void copyFile(File file, String destinPath) {
//		
//		  if(file.exists()) {
//			  File depath = new File(destinPath);
//				// Create destination folder,如果目标目录不存在,就创建目标目录,因为没有目录文件复制不过去的
//				if (!depath.exists()) {
//					depath.mkdirs();
//				}
//
//		        BufferedInputStream binput = null;
//		        BufferedOutputStream boutput = null;
//				FileInputStream fis = null;
//				FileOutputStream fos = null;
//			
//				
//				// 输入输出流的两个常用构造函数,其中在用来了一个字段File.separator,先用输入流读取文件,然后用输出流写文件到目标位置,完成复制功能
//				try {
//					fis = new FileInputStream(file);
//					fos = new FileOutputStream(depath +File.separator+ file.getName());
//		            binput = new BufferedInputStream(fis);
//		            boutput = new BufferedOutputStream(fos);
//					byte[] b = new byte[1024];
//					for (int i = 0; (i = binput.read(b)) != -1;) {
//						boutput.write(b, 0, i);
//						boutput.flush();
//					}
//					System.out.println("文件："+file.getName()+"已备份成功至："+destinPath+"目录下");
//					
//				} catch (Exception e) {
//					
//					System.out.println("哎呀 在这里没有文件了，无文件处理");
//					
//				} finally {
//					// close the Stream关闭资源啊,什么异常处理的就不写,自己补上吧
//					try {
//						boutput.close();
//						binput.close();
//						fos.close();
//						fis.close();
//					} catch (IOException e) {
//						System.out.println("无文件处理");
//					}
//					
//				}
//		  }
//	  }
		
	
	//复制文件
	  public boolean copyFile(File file, String destinPath) throws IOException {
		  
		  File depath = new File(destinPath);
			// Create destination folder,如果目标目录不存在,就创建目标目录,因为没有目录文件复制不过去的
			if (!depath.exists()) {
				depath.mkdirs();
			}

			BufferedInputStream binput = null;
			BufferedOutputStream boutput = null;
			FileInputStream fis = null;
			FileOutputStream fos = null;
		
			// 输入输出流的两个常用构造函数,其中在用来了一个字段File.separator,先用输入流读取文件,然后用输出流写文件到目标位置,完成复制功能
			try {
				fis = new FileInputStream(file);
				fos = new FileOutputStream(depath +File.separator+ file.getName());
				binput = new BufferedInputStream(fis);
				boutput = new BufferedOutputStream(fos);
				byte[] b = new byte[1024];
				for (int i = 0; (i = binput.read(b)) != -1;) {
					boutput.write(b, 0, i);
					boutput.flush();
				}
				System.out.println("文件："+file+"已备份成功至："+destinPath+"目录下");
				
				return true;
			} catch (Exception e) {
				
				e.printStackTrace();
			}
			finally {
				// close the Stream关闭资源啊,什么异常处理的就不写,自己补上吧
				try {
					boutput.close();
					binput.close();
					fos.close();
					fis.close();
				} catch (IOException e) {

					e.printStackTrace();
				}
				
			}
			return false;
		  
	  }
	  
		
	
	//多个文件导入数据库
	@SuppressWarnings("static-access")
	public void batchXmlInEbs(String localPath, String fileFormat, String backupPath, boolean del, String flag) throws Exception {

		List<File> listfile = this.FileSort(localPath); //按照文件的修改日期大小实现排序

		if(listfile != null && listfile.size() > 0) {
         	for (File file : listfile) {
     			
     			if (fileFormat != null && !"".equals(fileFormat.trim())) {  //判断文件的格式, 如果有,就执行下面语句
     				if (file.getName().startsWith(fileFormat)) {
     					
     					if (this.XmlInEbs(backupPath, file, flag) && del) {
     						
	     						this.deletelocal(localPath, file.getName());
		     					System.out.println("文件夹："+localPath+"下的文件已全部导入数据库成功！");
     					}

     				}
     			} else {//如果没有文件的格式，就执行下面语句
     				if (this.XmlInEbs(backupPath, file, flag) && del) {
	     					this.deletelocal(localPath, file.getName());
	     					System.out.println("文件夹："+localPath+"下的文件已全部导入数据库成功！");
     				}
     			}

     		}
         }
		
		else {
         	System.out.println("目录："+localPath+"下，无文件！无法插入数据库！");
         }
	 	
	}
	
	
	public void batchXmlInEbs1(boolean del) throws Exception {

		
		Properties pro=new Properties();
		InputStream in = OraDbUtil.class.getClassLoader().getResourceAsStream("properties.properties");
		pro.load(in);
		String gj_in = pro.getProperty("GJ_IN");
	 	
	 	String IN_VMI_BK = pro.getProperty("IN_VMI_BK"); //本地备份已下载的文件
	 	String IN_ORDEXE_BK = pro.getProperty("IN_ORDEXE_BK"); //本地备份已下载的文件
	 	String IN_ACK_ORDER_BK =pro.getProperty("IN_ACK_ORDER_BK"); //本地备份已下载的文件
		
		  
		 try
		    {
		      File file = new File(gj_in);
		      File[] files = file.listFiles(); //取得该路径下的所有文件
		      if(files.length > 0) {
		    	  for (int i = 0; i < files.length; i++)
		          {
		    		  if (files[i].isDirectory()) {
		    			  
		    			  
		    			  if (files[i].getName().equals("IN_ACK_ORDER")) {
		    				  
		    				  File file1 = new File(files[i].getPath());
		    			      File[] files1 = file1.listFiles(); //取得该路径下的所有文件
		    			      if(files1.length > 0) {
		    			    	  for (int j = 0; j < files1.length; j++)
		    			          {
		    			            if (files1[j].isFile()
		    			                && files1[j].getName().indexOf("bak") == -1)
		    			            {
		    			            	if(XmlInEbs1(files1[j],"ORDER")) {
			    			            	  System.out.println(files1[j]);
			    			            	  if(copyFile(files1[j],IN_ACK_ORDER_BK) && del) {
			    			            		  try {
			    			            			  files1[j].delete();
			    			            			  System.out.println("文件："+files1[j]+"已删除，文件大小："+files1[j].length()+"字节");
			    			            		  }catch(Exception ee) {
			    			            			  ee.printStackTrace();
			    			            		  }
			    			            		  
			    			            	  }
		    			            	}
		    			             
		    			            }
		    			            
		    			          } 
		    			    	  if (log.isInfoEnabled())
		    			          {
		    			            log.info("文件插入数据库成功！, 文件个数： "
		    			                + files1.length);
		    			          }
		    			      }
		    			      else {
		    			        	System.out.println("该文件夹："+files[i].getPath()+"下，无文件信息，请导入文件再上传！！");
		    			        }
			    		  }
			    		  else if (files[i].getName().equals("IN_VMI")) {
			    			  File file1 = new File(files[i].getPath());
		    			      File[] files1 = file1.listFiles(); //取得该路径下的所有文件
		    			      if(files1.length > 0) {
		    			    	  for (int j = 0; j < files1.length; j++)
		    			          {
		    			            if (files1[j].isFile()
		    			                && files1[j].getName().indexOf("bak") == -1)
		    			            {
		    			            	if(XmlInEbs1(files1[j],"VMI")) {
		    			              
			    			            	  System.out.println(files1[j]);
			    			                  if(copyFile(files1[j],IN_VMI_BK) && del) {
			    			                	  try {
			    			            			  files1[j].delete();
				    			          				System.out.println("文件："+files1[j]+"已删除，文件大小："+files1[j].length()+"字节");
			    			            		  }catch(Exception ee) {
			    			            			  ee.printStackTrace();
			    			            		  }
			    			                  }
		    			            	}
		    			              
		    			            }
		    			            
		    			          } 
		    			    	  if (log.isInfoEnabled())
		    			          {
		    			            log.info("文件插入数据库成功！, 文件个数： "
		    			                + files1.length);
		    			          }
		    			      }
		    			      else {
		    			        	System.out.println("该文件夹："+files[i].getPath()+"下，无文件信息，请导入文件再上传！！");
		    			        }
			    		  }
			    		  else if (files[i].getName().equals("IN_ORDEXE")) {
			    			  File file1 = new File(files[i].getPath());
		    			      File[] files1 = file1.listFiles(); //取得该路径下的所有文件
		    			      if(files1.length > 0) {
		    			    	  for (int j = 0; j < files1.length; j++)
		    			          {
		    			            if (files1[j].isFile()
		    			                && files1[j].getName().indexOf("bak") == -1)
		    			            {
		    			              
		    			            	if(XmlInEbs1(files1[j],"ORDEXE")) {

			    			            	  System.out.println(files1[j]);
			    			            	  if(copyFile(files1[j],IN_ORDEXE_BK) && del) {
			    			            		  try {
			    			            			  
			    			            			  files1[j].delete();
				    			          				System.out.println("文件："+files1[j]+"已删除，文件大小："+files1[j].length()+"字节");
			    			            		  }catch(Exception ee) {
			    			            			  ee.printStackTrace();
			    			            		  }
			    			            		  
			    			                  }
		    			            	}
		    			              
		    			            	
		    			            }
		    			            
		    			          } 
		    			    	  if (log.isInfoEnabled())
		    			          {
		    			            log.info("文件插入数据库成功！, 文件个数： "
		    			                + files1.length);
		    			          }
		    			      }
		    			      else {
		    			        	System.out.println("该文件夹："+files[i].getPath()+"下，无文件信息，请导入文件再上传！！");
		    			      }
			    		  }
		    		  }
		          } 
		      }
		      else {
		      	System.out.println("本地文件夹："+gj_in+"下，无目录信息，请确定文件目录！！");
		      }
		      
		    }
		    catch (Exception e)
		    {
		        e.printStackTrace();
		    }
		    finally
		    {
		    	in.close();
		    	pro.clear();
		    }
	 	
	}
	
	
	
	
	
	
	
	
	
	/**
	     * 导入后后删除本地文件
	* @param directory 要删除文件所在目录 
	* @param deleteFile 要删除的文件
	* @throws SftpException 
	*/
	public static void deletelocal(String directory, String deleteFile){
	
		File localfile = new File(directory+deleteFile);
		if(!localfile.exists()) {
			System.out.println("文件不存在");
		}
		else if (!localfile.isFile()) {
			System.out.println("请选择文件");
		}
		else{
			localfile.delete();
			System.out.println("文件："+localfile.getName()+"已删除");
		}
	
	}
	
	
	
	/**
	 * 
	 * 获取目录下所有文件
	 * 
	 * @param realpath
	 * @param files
	 * @return
	 */
	public static List<File> getFiles(String path) {
        List<File> files = new ArrayList<File>();
        File file = new File(path);
        File[] tempList = file.listFiles();

        for (int i = 0; i < tempList.length; i++) {
            if (tempList[i].isFile()) {
                files.add(tempList[i]);

            }
        }
        return files;
    }

	
	
	/**
	 * 返回排序后的文件列表(按时间排序)
	 * 
	 * @param path(目录)
	 * @return
	 */
	public static List<File> FileSort(String path) {

		List<File> list = getFiles(path);

		if (list != null && list.size() > 0) {

			Collections.sort(list, new Comparator<File>() {
				public int compare(File file, File newFile) {
					if (file.lastModified() < newFile.lastModified()) {
						return 1;
					} else if (file.lastModified() == newFile.lastModified()) {
						return 0;
					} else {
						return -1;
					}

				}
			});

		}

		return list;
	}
	


	public void in() throws Exception {
		
		System.out.println("xml文件开始导入数据库...");
//    	Properties pro=new Properties();
//		InputStream stream = OraDbUtil.class.getClassLoader().getResourceAsStream("properties.properties");
//		pro.load(stream);
//		
//		String IN_VMI = pro.getProperty("IN_VMI"); //本地保存服务器文件的路径,即:将导入数据库的文件目录
//	 	String IN_ORDEXE = pro.getProperty("IN_ORDEXE"); //本地保存服务器文件的路径
//	 	String IN_ACK_ORDER =pro.getProperty("IN_ACK_ORDER"); //本地保存服务器文件的路径
//	 	
//	 	String IN_VMI_BK = pro.getProperty("IN_VMI_BK"); //本地备份已下载的文件
//	 	String IN_ORDEXE_BK = pro.getProperty("IN_ORDEXE_BK"); //本地备份已下载的文件
//	 	String IN_ACK_ORDER_BK =pro.getProperty("IN_ACK_ORDER_BK"); //本地备份已下载的文件
//		
//		batchXmlInEbs(IN_VMI, null, IN_VMI_BK, true, "VMI");
//		batchXmlInEbs(IN_ORDEXE, null, IN_ORDEXE_BK, true, "ORDEXE");
//		batchXmlInEbs(IN_ACK_ORDER, null, IN_ACK_ORDER_BK, true, "ORDER");
//	 	
//		stream.close();
		
		batchXmlInEbs1(true);
		
	}
	

	
}