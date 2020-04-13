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

	
	//��������
	public static Connection getConnection() throws IOException {
		
//		Properties pro=new Properties();
//		InputStream in = GJSFTP.class.getClassLoader().getResourceAsStream("properties.properties");
//		pro.load(in);
//		DB_URL = pro.getProperty("OUT_VMI"); //��Զ��������·������
//		DB_USER = pro.getProperty("OUT_VMI");
//		DB_PASSWORD = pro.getProperty("OUT_VMI");
		try {
			if (conn == null) {
				Class.forName("oracle.jdbc.driver.OracleDriver");

				conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
				System.out.println("���ݿ����ӳɹ���");
			}
		

			return conn;
		} catch (Exception e) {
			e.printStackTrace();
		}

		return null;
	}
	
	//��ȡxml�����ݣ�
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
	
    
	// ����CLOBת��STRING����
	public String ClobToString(CLOB clob) throws SQLException,IOException {

		String reString = "";
	
		Reader is = clob.getCharacterStream();// �õ���
	
		BufferedReader br = new BufferedReader(is);
	
		String s = br.readLine();
	
		StringBuffer sb = new StringBuffer();
	
		while (s != null) {// ִ��ѭ�����ַ���ȫ��ȡ����ֵ��StringBuffer��StringBufferת��STRING
	
			sb.append(s);
		
			s = br.readLine();
	
		}
	
		reString = sb.toString();
	
		return reString;

	}
	
	
	/**
	* ��clob�ֶ�
	*/
	@SuppressWarnings("unused")
	public void XmlOutEbs() throws IOException, SQLException {
		
    	Properties pro=new Properties();
		InputStream stream = OraDbUtil.class.getClassLoader().getResourceAsStream("properties.properties");
		pro.load(stream);
		String out_order =  pro.getProperty("OUT_ORDER"); //�����ļ��ϴ���·��
        String out_ack_vmi =  pro.getProperty("OUT_ACK_VMI"); //�����ļ��ϴ���·��
        String out_ack_ordexe =  pro.getProperty("OUT_ACK_ORDEXE"); //�����ļ��ϴ���·��

        Connection conn = null;
        String sql = null;
        PreparedStatement  stmt = null;
        ResultSet rs = null;
        
        conn = getConnection(); //��������
		
		sql = "SELECT XML_NAME, XML_CONTENT, REQUEST_TYPE FROM XML_OUT_DB WHERE XML_STATUS = 1 for update";
		
		stmt = conn.prepareStatement(sql);
		rs = stmt.executeQuery();        //ִ�в�ѯ���
        
		try {
			
			
			while (rs.next()) { // ȡ��CLOB����
				String filename = null;
				CLOB clob = null;
				String flag = null;
				String content = null;
				filename = rs.getString("XML_NAME"); //��ȡ�ļ���
				clob = (CLOB) rs.getClob("XML_CONTENT"); //��ȡCLOB����
				flag = rs.getString("REQUEST_TYPE"); //��ȡ�ļ�����������

				content = ClobToString(clob); //��CLOB����ת��String
				
				if(flag.equals("VMI")) {
					BufferedWriter out1 = null;
					out1 = new BufferedWriter(new FileWriter(out_ack_vmi + filename));
					out1.write(content); //д���ļ�
					System.out.println("���ݿ�ɹ�����xml�ļ���"+filename+"����Ŀ¼��" + out_ack_vmi);
					out1.flush(); 
					
					//�����ݶ�ȡ֮�󣬽�������״̬���£������ظ���ȡ
					String updatesql = "UPDATE XML_OUT_DB SET XML_STATUS = 2 where XML_NAME = '"+filename+"'";
					PreparedStatement stmt2 = conn.prepareStatement(updatesql);
					ResultSet rs2 = stmt2.executeQuery();        //ִ�в�ѯ���
					try {
						out1.close();
					}catch(Exception ee) {
						ee.printStackTrace();
					}
					
				}
				else if(flag.equals("ORDEXE")) {
					BufferedWriter out1 = null;
					out1 = new BufferedWriter(new FileWriter(out_ack_ordexe + filename));
					out1.write(content); //д���ļ�
					System.out.println("���ݿ�ɹ�����xml�ļ���"+filename+"����Ŀ¼��" + out_ack_ordexe);
					out1.flush(); 
					
					//�����ݶ�ȡ֮�󣬽�������״̬���£������ظ���ȡ
					String updatesql = "UPDATE XML_OUT_DB SET XML_STATUS = 2 where XML_NAME = '"+filename+"'";
					PreparedStatement stmt2 = conn.prepareStatement(updatesql);
					ResultSet rs2 = stmt2.executeQuery();        //ִ�в�ѯ���
					try {
						out1.close();
					}catch(Exception ee) {
						ee.printStackTrace();
					}
					
				}
				else if(flag.equals("ORDER")) {
					BufferedWriter out1 = null;
					out1 = new BufferedWriter(new FileWriter(out_order + filename));
					out1.write(content); //д���ļ�
					System.out.println("���ݿ�ɹ�����xml�ļ���"+filename+"����Ŀ¼��" + out_order);
					out1.flush(); 
					
					//�����ݶ�ȡ֮�󣬽�������״̬���£������ظ���ȡ
					String updatesql = "UPDATE XML_OUT_DB SET XML_STATUS = 2 where XML_NAME = '"+filename+"'";
					PreparedStatement stmt2 = conn.prepareStatement(updatesql);
					ResultSet rs2 = stmt2.executeQuery();        //ִ�в�ѯ���
					try {
						out1.close();
					}catch(Exception ee) {
						ee.printStackTrace();
					}
					
				}
			}
			
		}catch(Exception e) {
			e.printStackTrace();
			System.out.println("���ݿ���û�пɵ���������"); 
		}
		finally {
			
	        stream.close();
		}
	}
	
	//�����ļ��������ݿ�
	public boolean XmlInEbs(String backupPath, File file, String type) throws SQLException, IOException {

		
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		String filename = file.getName();
//		String inFile = localPath + file; //inFileΪ�ļ�ȫ·��
		String date = sdf.format(new Date(file.lastModified()));
		int status = 0;
	
		Connection conn = getConnection(); //��������
	 	
	 	conn.setAutoCommit(false);

		String sql = "INSERT INTO XML_IN_DB (XML_NAME, XML_CONTENT, CREATE_DATE, REQUEST_TYPE, XML_STATUS) values (?,?,?,?,?)";
		
		CLOB clob = new CLOB((OracleConnection)conn);
		clob = CLOB.createTemporary((OracleConnection)conn,true,1);
		clob.setString(1,readFileContent(file)); //��ȡ�ļ����ַ���ת��CLOB
		OracleConnection OCon = (OracleConnection)conn;
        OraclePreparedStatement ps = (OraclePreparedStatement)OCon.prepareCall(sql);
	
        ps.setString(1, filename); //��������
        ps.setClob(2, clob); //����CLOB����
        ps.setString(3, date); //����ʱ��
        
        if(type.equals("VMI")) {
        	ps.setString(4, type); //������������    
		}
		else if(type.equals("ORDEXE")) {
			ps.setString(4, type); //������������
		}
		else if(type.equals("ORDER")) {
			ps.setString(4, type); //������������
		} 
        
        ps.setInt(5, status); //���봦��״̬

        ps.executeUpdate();
        ps.close();
        ps = null;
        
        OCon.commit();
        conn.setAutoCommit(true);
        
        this.copyFile(file, backupPath);
        
        System.out.println("�ļ�:" + file + "�ѵ������ݿ⣬������");

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
	 		conn = getConnection(); //��������
			conn.setAutoCommit(false);
			sql = "INSERT INTO XML_IN_DB (XML_NAME, XML_CONTENT, CREATE_DATE, REQUEST_TYPE, XML_STATUS) values (?,?,?,?,?)";
			
			clob = new CLOB((OracleConnection)conn);
			clob = CLOB.createTemporary((OracleConnection)conn,true,1);
			clob.setString(1,readFileContent(file)); //��ȡ�ļ����ַ���ת��CLOB
			OracleConnection OCon = (OracleConnection)conn;
	        OraclePreparedStatement ps = (OraclePreparedStatement)OCon.prepareCall(sql);
		
	        ps.setString(1, filename); //��������
	        ps.setClob(2, clob); //����CLOB����
	        ps.setString(3, date); //����ʱ��
	        ps.setString(4, type); //������������   
	        ps.setInt(5, status); //���봦��״̬

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
	
	
	
	
	
	
	
	
	
	
		
	//�����ļ�
//	  public void copyFile(File file, String destinPath) {
//		
//		  if(file.exists()) {
//			  File depath = new File(destinPath);
//				// Create destination folder,���Ŀ��Ŀ¼������,�ʹ���Ŀ��Ŀ¼,��Ϊû��Ŀ¼�ļ����Ʋ���ȥ��
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
//				// ������������������ù��캯��,������������һ���ֶ�File.separator,������������ȡ�ļ�,Ȼ���������д�ļ���Ŀ��λ��,��ɸ��ƹ���
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
//					System.out.println("�ļ���"+file.getName()+"�ѱ��ݳɹ�����"+destinPath+"Ŀ¼��");
//					
//				} catch (Exception e) {
//					
//					System.out.println("��ѽ ������û���ļ��ˣ����ļ�����");
//					
//				} finally {
//					// close the Stream�ر���Դ��,ʲô�쳣����ľͲ�д,�Լ����ϰ�
//					try {
//						boutput.close();
//						binput.close();
//						fos.close();
//						fis.close();
//					} catch (IOException e) {
//						System.out.println("���ļ�����");
//					}
//					
//				}
//		  }
//	  }
		
	
	//�����ļ�
	  public boolean copyFile(File file, String destinPath) throws IOException {
		  
		  File depath = new File(destinPath);
			// Create destination folder,���Ŀ��Ŀ¼������,�ʹ���Ŀ��Ŀ¼,��Ϊû��Ŀ¼�ļ����Ʋ���ȥ��
			if (!depath.exists()) {
				depath.mkdirs();
			}

			BufferedInputStream binput = null;
			BufferedOutputStream boutput = null;
			FileInputStream fis = null;
			FileOutputStream fos = null;
		
			// ������������������ù��캯��,������������һ���ֶ�File.separator,������������ȡ�ļ�,Ȼ���������д�ļ���Ŀ��λ��,��ɸ��ƹ���
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
				System.out.println("�ļ���"+file+"�ѱ��ݳɹ�����"+destinPath+"Ŀ¼��");
				
				return true;
			} catch (Exception e) {
				
				e.printStackTrace();
			}
			finally {
				// close the Stream�ر���Դ��,ʲô�쳣����ľͲ�д,�Լ����ϰ�
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
	  
		
	
	//����ļ��������ݿ�
	@SuppressWarnings("static-access")
	public void batchXmlInEbs(String localPath, String fileFormat, String backupPath, boolean del, String flag) throws Exception {

		List<File> listfile = this.FileSort(localPath); //�����ļ����޸����ڴ�Сʵ������

		if(listfile != null && listfile.size() > 0) {
         	for (File file : listfile) {
     			
     			if (fileFormat != null && !"".equals(fileFormat.trim())) {  //�ж��ļ��ĸ�ʽ, �����,��ִ���������
     				if (file.getName().startsWith(fileFormat)) {
     					
     					if (this.XmlInEbs(backupPath, file, flag) && del) {
     						
	     						this.deletelocal(localPath, file.getName());
		     					System.out.println("�ļ��У�"+localPath+"�µ��ļ���ȫ���������ݿ�ɹ���");
     					}

     				}
     			} else {//���û���ļ��ĸ�ʽ����ִ���������
     				if (this.XmlInEbs(backupPath, file, flag) && del) {
	     					this.deletelocal(localPath, file.getName());
	     					System.out.println("�ļ��У�"+localPath+"�µ��ļ���ȫ���������ݿ�ɹ���");
     				}
     			}

     		}
         }
		
		else {
         	System.out.println("Ŀ¼��"+localPath+"�£����ļ����޷��������ݿ⣡");
         }
	 	
	}
	
	
	public void batchXmlInEbs1(boolean del) throws Exception {

		
		Properties pro=new Properties();
		InputStream in = OraDbUtil.class.getClassLoader().getResourceAsStream("properties.properties");
		pro.load(in);
		String gj_in = pro.getProperty("GJ_IN");
	 	
	 	String IN_VMI_BK = pro.getProperty("IN_VMI_BK"); //���ر��������ص��ļ�
	 	String IN_ORDEXE_BK = pro.getProperty("IN_ORDEXE_BK"); //���ر��������ص��ļ�
	 	String IN_ACK_ORDER_BK =pro.getProperty("IN_ACK_ORDER_BK"); //���ر��������ص��ļ�
		
		  
		 try
		    {
		      File file = new File(gj_in);
		      File[] files = file.listFiles(); //ȡ�ø�·���µ������ļ�
		      if(files.length > 0) {
		    	  for (int i = 0; i < files.length; i++)
		          {
		    		  if (files[i].isDirectory()) {
		    			  
		    			  
		    			  if (files[i].getName().equals("IN_ACK_ORDER")) {
		    				  
		    				  File file1 = new File(files[i].getPath());
		    			      File[] files1 = file1.listFiles(); //ȡ�ø�·���µ������ļ�
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
			    			            			  System.out.println("�ļ���"+files1[j]+"��ɾ�����ļ���С��"+files1[j].length()+"�ֽ�");
			    			            		  }catch(Exception ee) {
			    			            			  ee.printStackTrace();
			    			            		  }
			    			            		  
			    			            	  }
		    			            	}
		    			             
		    			            }
		    			            
		    			          } 
		    			    	  if (log.isInfoEnabled())
		    			          {
		    			            log.info("�ļ��������ݿ�ɹ���, �ļ������� "
		    			                + files1.length);
		    			          }
		    			      }
		    			      else {
		    			        	System.out.println("���ļ��У�"+files[i].getPath()+"�£����ļ���Ϣ���뵼���ļ����ϴ�����");
		    			        }
			    		  }
			    		  else if (files[i].getName().equals("IN_VMI")) {
			    			  File file1 = new File(files[i].getPath());
		    			      File[] files1 = file1.listFiles(); //ȡ�ø�·���µ������ļ�
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
				    			          				System.out.println("�ļ���"+files1[j]+"��ɾ�����ļ���С��"+files1[j].length()+"�ֽ�");
			    			            		  }catch(Exception ee) {
			    			            			  ee.printStackTrace();
			    			            		  }
			    			                  }
		    			            	}
		    			              
		    			            }
		    			            
		    			          } 
		    			    	  if (log.isInfoEnabled())
		    			          {
		    			            log.info("�ļ��������ݿ�ɹ���, �ļ������� "
		    			                + files1.length);
		    			          }
		    			      }
		    			      else {
		    			        	System.out.println("���ļ��У�"+files[i].getPath()+"�£����ļ���Ϣ���뵼���ļ����ϴ�����");
		    			        }
			    		  }
			    		  else if (files[i].getName().equals("IN_ORDEXE")) {
			    			  File file1 = new File(files[i].getPath());
		    			      File[] files1 = file1.listFiles(); //ȡ�ø�·���µ������ļ�
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
				    			          				System.out.println("�ļ���"+files1[j]+"��ɾ�����ļ���С��"+files1[j].length()+"�ֽ�");
			    			            		  }catch(Exception ee) {
			    			            			  ee.printStackTrace();
			    			            		  }
			    			            		  
			    			                  }
		    			            	}
		    			              
		    			            	
		    			            }
		    			            
		    			          } 
		    			    	  if (log.isInfoEnabled())
		    			          {
		    			            log.info("�ļ��������ݿ�ɹ���, �ļ������� "
		    			                + files1.length);
		    			          }
		    			      }
		    			      else {
		    			        	System.out.println("���ļ��У�"+files[i].getPath()+"�£����ļ���Ϣ���뵼���ļ����ϴ�����");
		    			      }
			    		  }
		    		  }
		          } 
		      }
		      else {
		      	System.out.println("�����ļ��У�"+gj_in+"�£���Ŀ¼��Ϣ����ȷ���ļ�Ŀ¼����");
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
	     * ������ɾ�������ļ�
	* @param directory Ҫɾ���ļ�����Ŀ¼ 
	* @param deleteFile Ҫɾ�����ļ�
	* @throws SftpException 
	*/
	public static void deletelocal(String directory, String deleteFile){
	
		File localfile = new File(directory+deleteFile);
		if(!localfile.exists()) {
			System.out.println("�ļ�������");
		}
		else if (!localfile.isFile()) {
			System.out.println("��ѡ���ļ�");
		}
		else{
			localfile.delete();
			System.out.println("�ļ���"+localfile.getName()+"��ɾ��");
		}
	
	}
	
	
	
	/**
	 * 
	 * ��ȡĿ¼�������ļ�
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
	 * �����������ļ��б�(��ʱ������)
	 * 
	 * @param path(Ŀ¼)
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
		
		System.out.println("xml�ļ���ʼ�������ݿ�...");
//    	Properties pro=new Properties();
//		InputStream stream = OraDbUtil.class.getClassLoader().getResourceAsStream("properties.properties");
//		pro.load(stream);
//		
//		String IN_VMI = pro.getProperty("IN_VMI"); //���ر���������ļ���·��,��:���������ݿ���ļ�Ŀ¼
//	 	String IN_ORDEXE = pro.getProperty("IN_ORDEXE"); //���ر���������ļ���·��
//	 	String IN_ACK_ORDER =pro.getProperty("IN_ACK_ORDER"); //���ر���������ļ���·��
//	 	
//	 	String IN_VMI_BK = pro.getProperty("IN_VMI_BK"); //���ر��������ص��ļ�
//	 	String IN_ORDEXE_BK = pro.getProperty("IN_ORDEXE_BK"); //���ر��������ص��ļ�
//	 	String IN_ACK_ORDER_BK =pro.getProperty("IN_ACK_ORDER_BK"); //���ر��������ص��ļ�
//		
//		batchXmlInEbs(IN_VMI, null, IN_VMI_BK, true, "VMI");
//		batchXmlInEbs(IN_ORDEXE, null, IN_ORDEXE_BK, true, "ORDEXE");
//		batchXmlInEbs(IN_ACK_ORDER, null, IN_ACK_ORDER_BK, true, "ORDER");
//	 	
//		stream.close();
		
		batchXmlInEbs1(true);
		
	}
	

	
}