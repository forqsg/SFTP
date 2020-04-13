package com.gj.sftp;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.Vector;
import org.apache.log4j.Logger;
import org.springframework.context.support.FileSystemXmlApplicationContext;

import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.SftpATTRS;
import com.jcraft.jsch.SftpException;
import com.jcraft.jsch.ChannelSftp.LsEntry;
/**
 * sftp������
 * 
 * @author qsg
 * @date 2019-10-09
 * @time ����1:39:44
 * @version 2.0
 */
public class GJSFTP 
{
  private static Logger log = Logger.getLogger(GJSFTP.class.getName());
 
  private String host;//����������ip
  private String username;//�û���
  private int port;//�˿ں�
  String privateKey;
  
  Session session = null;   
  Channel channel = null;   
  ChannelSftp sftp = null;// sftp������  
 
  public GJSFTP(){}
 
 
  public String getHost()
  {
    return host;
  }
 
  public void setHost(String host)
  {
    this.host = host;
  }
 
  public String getUsername()
  {
    return username;
  }
 
  public void setUsername(String username)
  {
    this.username = username;
  }
 
  public int getPort()
  {
    return port;
  }
 
  public void setPort(int port)
  {
    this.port = port;
  }
 
  public ChannelSftp getSftp()
  {
    return sftp;
  }
 
  public void setSftp(ChannelSftp sftp)
  {
    this.sftp = sftp;
  }

  /**
   * ͨ��SFTP���ӷ�����
 * @throws JSchException 
 * @throws IOException 
   */
  public ChannelSftp connect() throws JSchException, IOException
  {
	  
	  Properties pro=new Properties();
	  InputStream in = GJSFTP.class.getClassLoader().getResourceAsStream("properties.properties");
	  pro.load(in);
		
	    /** ���� */ 
	  String host = pro.getProperty("host"); 
	    /** �˿� */ 
	  int port = Integer.parseInt(pro.getProperty("port")); 
	    /** �û��� */ 
	  String username = pro.getProperty("username");
	    /**��Կ��ַ*/   
	  String privateKey = pro.getProperty("privateKey");

	  JSch jsch = new JSch();   
	  
      //֧����Կ�ķ�ʽ��½�� 

    //���ò����������Կ  
       jsch.addIdentity(privateKey);  
 
      session = jsch.getSession(username, host, port);     
      session.setConfig("StrictHostKeyChecking", "no"); 
      try {
          session.connect();   
      } catch (Exception e) {
          if (session.isConnected())   
              session.disconnect();   
          log.error("���ӷ�����ʧ��,��������[" + host + "],�˿�[" + port   
                  + "],�û���[" + username + "],�˿�[" + port   
                  + "]�Ƿ���ȷ,������Ϣ��ȷ��������������������Ƿ������������󱻷���ǽ�ܾ�.");
          javax.swing.JOptionPane.showMessageDialog(null, "���ӷ�����ʧ��,��������[" + host + "],�˿�[" + port   
                  + "],�û���[" + username + "],�˿�[" + port   
                  + "]�Ƿ���ȷ,������Ϣ��ȷ��������������������Ƿ������������󱻷���ǽ�ܾ�.");
      }
      channel = (Channel)session.openChannel("sftp");//����sftpͨ��ͨ��  
      try {
          channel.connect();   
      } catch (Exception e) {   
          if (channel.isConnected())   
              channel.disconnect();   
          log.error("���ӷ�����ʧ��,��������[" + host + "],�˿�[" + port   
                  + "],�û���[" + username + "],��Կ�Ƿ���ȷ,������Ϣ��ȷ��������������������Ƿ������������󱻷���ǽ�ܾ�.");
          javax.swing.JOptionPane.showMessageDialog(null,"���ӷ�����ʧ��,��������[" + host + "],�˿�[" + port   
                  + "],�û���[" + username + "],��Կ�Ƿ���ȷ,������Ϣ��ȷ��������������������Ƿ������������󱻷���ǽ�ܾ�.");
      }
      sftp = (ChannelSftp) channel;   
      System.out.println(sftp+"���ӳɹ���");
      return sftp;
  }
  
  
  /**
   * �ر�����
   */
  public void disconnect()
  {
    if (this.sftp != null)
    {
      if (this.sftp.isConnected())
      {
        this.sftp.disconnect();
        if (log.isInfoEnabled())
        {
          log.info("sftp is closed already");
        }
      }
    }
    if (this.session != null)
    {
      if (this.session.isConnected())
      {
        this.session.disconnect();
        if (log.isInfoEnabled())
        {
          log.info("session is closed already");
        }
      }
    }
  }
  
  
  
  
  
 
  /**
   * ���������ļ�
   */
  @SuppressWarnings("rawtypes")
	public List<String> batchDownLoadFile(String remotePath, String localPath,
	      String fileFormat, String fileEndFormat, boolean del) {
	  
	  	File localpath = new File(localPath);
		//�������Ŀ¼������,�ʹ�������Ŀ¼
		if (!localpath.exists()) {
			localpath.mkdirs();
		}
		
	    List<String> filenames = new ArrayList<String>();
	    try
	    {
	      // connect();
	      Vector v = listFiles(remotePath);
	      // sftp.cd(remotePath);
	      if (v.size() > 0)
	      {
	        System.out.println("���δ����ļ�������Ϊ��,��ʼ����...�ļ�����Ϊ��" + v.size());
	        Iterator it = v.iterator();
	        while (it.hasNext())
	        {
	          LsEntry entry = (LsEntry) it.next();
	          String filename = entry.getFilename();
	          SftpATTRS attrs = entry.getAttrs();
	          if (!attrs.isDir())
	          {
	            boolean flag = false;
	            String localFileName = localPath + filename;
	            fileFormat = fileFormat == null ? "" : fileFormat
	                .trim();
	            fileEndFormat = fileEndFormat == null ? ""
	                : fileEndFormat.trim();
	            // �������
	            if (fileFormat.length() > 0 && fileEndFormat.length() > 0)
	            {
	              if (filename.startsWith(fileFormat) && filename.endsWith(fileEndFormat))
	              {
	                flag = downloadFile(remotePath, filename,localPath, filename);
	                if (flag)
	                {
	                  filenames.add(localFileName);
	                  if (flag && del)
	                  {
	                    deleteSFTP(remotePath, filename);
	                  }
	                }
	              }
	            }
	            else if (fileFormat.length() > 0 && "".equals(fileEndFormat))
	            {
	              if (filename.startsWith(fileFormat))
	              {
	                flag = downloadFile(remotePath, filename, localPath, filename);
	                if (flag)
	                {
	                  filenames.add(localFileName);
	                  if (flag && del)
	                  {
	                    deleteSFTP(remotePath, filename);
	                  }
	                }
	              }
	            }
	            else if (fileEndFormat.length() > 0 && "".equals(fileFormat))
	            {
	              if (filename.endsWith(fileEndFormat))
	              {
	                flag = downloadFile(remotePath, filename,localPath, filename);
	                if (flag)
	                {
	                  filenames.add(localFileName);
	                  if (flag && del)
	                  {
	                    deleteSFTP(remotePath, filename);
	                  }
	                }
	              }
	            }
	            else
	            {
	              flag = downloadFile(remotePath, filename,localPath, filename);
	              if (flag)
	              {
	                filenames.add(localFileName);
	                if (flag && del)
	                {
	                  deleteSFTP(remotePath, filename);
	                }
	              }
	            }
	          }
	        }
	      }
	      if (log.isInfoEnabled())
	      {
	        log.info("�ѳɹ������ļ���Զ�̵�ַ=" + remotePath
	            + "   ���ص�ַ=" + localPath + ",�ļ�����Ϊ"
	            + v.size());
	      }
	    }
	    catch (SftpException e)
	    {
	      e.printStackTrace();
	    }
	    finally
	    {
	      // this.disconnect();
	    }
	    return filenames;
	 }
	 
  /**
   * ���ص����ļ�
   */
  public boolean downloadFile(String remotePath, String remoteFileName,String localPath, String localFileName)
  {
    FileOutputStream fieloutput = null;
    try
    {
      sftp.cd(remotePath);
      fieloutput = new FileOutputStream(new File(localPath,remoteFileName));
      sftp.get(remotePath + remoteFileName, fieloutput);
      if (log.isInfoEnabled())
      {
        log.info("Զ���ļ�:" + remoteFileName + " ���سɹ�.");
      }
      return true;
    }
    catch (FileNotFoundException e)
    {
      e.printStackTrace();
    }
    catch (SftpException e)
    {
      e.printStackTrace();
    }
    finally
    {
      if (null != fieloutput)
      {
        try
        {
          fieloutput.close();
        }
        catch (IOException e)
        {
          e.printStackTrace();
        }
      }
    }
    return false;
  }
 
  /**
   * �ϴ������ļ�
   */
  
  public boolean uploadFile(String remotePath, String remoteFileName,
		  String localPath, String localFileName) throws IOException
  {
	  FileInputStream in = null;
	    try
	    {
	      createDir(remotePath);
	      File file = new File(localPath + localFileName);
	      in = new FileInputStream(file);
	      sftp.put(in, remoteFileName);
	    
	      System.out.println("�ļ���" + file+ " ���ϴ��ɹ����ļ���С��"+file.length()+"�ֽ�");

	      return true;
	    }
	    catch (FileNotFoundException e)
	    {
	      e.printStackTrace();
	    }
	    catch (SftpException e)
	    {
	      e.printStackTrace();
	    }finally {
			in.close();
		}
	    
	    return false;

 }
  
  
  /**
   * �����ϴ��ļ�
   */
 
  public void bacthUploadFile(boolean del) throws IOException {
     
	  Properties pro=new Properties();
	  InputStream in = GJSFTP.class.getClassLoader().getResourceAsStream("properties.properties");
	  pro.load(in);
	  String gj_out =  pro.getProperty("GJ_OUT");
	  
	  String in_order =  pro.getProperty("IN_ORDER");//�ϴ���Զ��������·��
	  String in_ack_vmi =  pro.getProperty("IN_ACK_VMI");//�ϴ���Զ��������·��
	  String in_ack_ordexe =  pro.getProperty("IN_ACK_ORDEXE");//�ϴ���Զ��������·��	
//	
//	  String out_order =  pro.getProperty("OUT_ORDER");//�����ļ��ϴ���·��
//	  String out_ack_vmi =  pro.getProperty("OUT_ACK_VMI");//�����ļ��ϴ���·��
//	  String out_ack_ordexe =  pro.getProperty("OUT_ACK_ORDEXE");//�����ļ��ϴ���·��
//	
	  String out_order_bk =  pro.getProperty("OUT_ORDER_BK");//���ر��ݵ�·��
	  String out_ack_vmi_bk =  pro.getProperty("OUT_ACK_VMI_BK");//���ر��ݵ�·��
	  String out_ack_ordexe_bk =  pro.getProperty("OUT_ACK_ORDEXE_BK");//���ر��ݵ�·��
	  
	 try
	    {
	      connect();
	      
	      File file = new File(gj_out);
	      File[] files = file.listFiles(); //ȡ�ø�·���µ������ļ�
	      if(files.length > 0) {
	    	  for (int i = 0; i < files.length; i++)
	          {
	    		  if (files[i].isDirectory()) {
	    			  
	    			  
	    			  if (files[i].getName().equals("OUT_ORDER")) {
	    				  
	    				  File file1 = new File(files[i].getPath());
	    			      File[] files1 = file1.listFiles(); //ȡ�ø�·���µ������ļ�
	    			      if(files1.length > 0) {
	    			    	  for (int j = 0; j < files1.length; j++)
	    			          {
	    			            if (files1[j].isFile()
	    			                && files1[j].getName().indexOf("bak") == -1)
	    			            {
	    			              if (this.uploadFile(in_order, files1[j].getName(),
	    			            		  files[i].getPath()+"\\", files1[j].getName()))
	    			              {
	    			            	  System.out.println(files1[j]);
	    			            	  if(copyFile(files1[j],out_order_bk) && del) {
	    			            		  
	    			            		  wait(1000);
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
	    			            log.info("�ϴ��ļ��ɹ�:Զ�̵�ַ=" + in_order
	    			                + "   ���ص�ַ=" + files[i].getPath() + ", �ļ������� "
	    			                + files1.length);
	    			          }
	    			      }
	    			      else {
	    			        	System.out.println("���ļ��У�"+files[i].getPath()+"�£����ļ���Ϣ���뵼���ļ����ϴ�����");
	    			        }
		    		  }
		    		  else if (files[i].getName().equals("OUT_ACK_VMI")) {
		    			  File file1 = new File(files[i].getPath());
	    			      File[] files1 = file1.listFiles(); //ȡ�ø�·���µ������ļ�
	    			      if(files1.length > 0) {
	    			    	  for (int j = 0; j < files1.length; j++)
	    			          {
	    			            if (files1[j].isFile()
	    			                && files1[j].getName().indexOf("bak") == -1)
	    			            {
	    			              if (this.uploadFile(in_ack_vmi, files1[j].getName(),
	    			            		  files[i].getPath()+"\\", files1[j].getName()))
	    			              {
	    			            	  System.out.println(files1[j]);
	    			                  if(copyFile(files1[j],out_ack_vmi_bk) && del) {
	    			                	  wait(1000);
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
	    			            log.info("�ϴ��ļ��ɹ�:Զ�̵�ַ=" + in_ack_vmi
	    			                + "   ���ص�ַ=" + files[i].getPath() + ", �ļ������� "
	    			                + files1.length);
	    			          }
	    			      }
	    			      else {
	    			        	System.out.println("���ļ��У�"+files[i].getPath()+"�£����ļ���Ϣ���뵼���ļ����ϴ�����");
	    			        }
		    		  }
		    		  else if (files[i].getName().equals("OUT_ACK_ORDEXE")) {
		    			  File file1 = new File(files[i].getPath());
	    			      File[] files1 = file1.listFiles(); //ȡ�ø�·���µ������ļ�
	    			      if(files1.length > 0) {
	    			    	  for (int j = 0; j < files1.length; j++)
	    			          {
	    			            if (files1[j].isFile()
	    			                && files1[j].getName().indexOf("bak") == -1)
	    			            {
	    			              if (this.uploadFile(in_ack_ordexe, files1[j].getName(),
	    			            		  files[i].getPath()+"\\", files1[j].getName()))
	    			              {

	    			            	  System.out.println(files1[j]);
	    			            	  if(copyFile(files1[j],out_ack_ordexe_bk) && del) {
	    			            		  wait(1000);
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
	    			            log.info("�ϴ��ļ��ɹ�:Զ�̵�ַ=" + in_ack_ordexe
	    			                + "   ���ص�ַ=" + files[i].getPath() + ", �ļ������� "
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
	      	System.out.println("���ļ��У�"+gj_out+"�£���Ŀ¼��Ϣ����ȷ���ļ�Ŀ¼����");
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
	        this.disconnect();
	    }
	  
}

  /**
   * �ϴ���ɾ�������ļ�
   */
  
  public void deletelocal(File file){
		try {
			if(!file.exists()) {
				System.out.println("�ļ�������");
			}
			else if (!file.isFile()) {
				System.out.println("��ѡ���ļ�");
			}
			else{
				file.delete();
				System.out.println("�ļ���"+file+"��ɾ��");
			}
		}catch(Exception e) {
			System.out.println("ɾ�������У������쳣");
			e.printStackTrace();
		}

	}
  
  
  
  /**
   * ����Ŀ¼
   */
  public boolean createDir(String createpath)
  {
    try
    {
      if (isDirExist(createpath))
      {
        this.sftp.cd(createpath);
        return true;
      }
      String pathArry[] = createpath.split("/");
      StringBuffer filePath = new StringBuffer("/");
      for (String path : pathArry)
      {
        if (path.equals(""))
        {
          continue;
        }
        filePath.append(path + "/");
        if (isDirExist(filePath.toString()))
        {
          sftp.cd(filePath.toString());
        }
        else
        {
          // ����Ŀ¼
          sftp.mkdir(filePath.toString());
          // ���벢����Ϊ��ǰĿ¼
          sftp.cd(filePath.toString());
        }
 
      }
      this.sftp.cd(createpath);
      return true;
    }
    catch (SftpException e)
    {
      e.printStackTrace();
    }
    return false;
  }
 
  /**
   * �ж�Ŀ¼�Ƿ����
   * @param directory
   * @return
   */
  public boolean isDirExist(String directory)
  {
    boolean isDirExistFlag = false;
    try
    {
      SftpATTRS sftpATTRS = sftp.lstat(directory);
      isDirExistFlag = true;
      return sftpATTRS.isDir();
    }
    catch (Exception e)
    {
      if (e.getMessage().toLowerCase().equals("no such file"))
      {
        isDirExistFlag = false;
      }
    }
    return isDirExistFlag;
  }
 
 
  /**
   * ɾ��stfp�ļ�
   * @param directory��Ҫɾ���ļ�����Ŀ¼
   * @param deleteFile��Ҫɾ�����ļ�
   * @param sftp
   */
  public void deleteSFTP(String directory, String deleteFile)
  {
    try
    {
      // sftp.cd(directory);
      sftp.rm(directory + deleteFile);
      if (log.isInfoEnabled())
      {
        log.info("delete file success from sftp.");
      }
    }
    catch (Exception e)
    {
      e.printStackTrace();
    }
  }

  /**
   * ���Ŀ¼�����ھʹ���Ŀ¼
   * @param path
   */
  public void mkdirs(String path)
  {
    File f = new File(path);
 
    String fs = f.getParent();
 
    f = new File(fs);
 
    if (!f.exists())
    {
      f.mkdirs();
    }
  }
 
  /**
   * �г�Ŀ¼�µ��ļ�
   * 
   * @param directory��Ҫ�г���Ŀ¼
   * @param sftp
   * @return
   * @throws SftpException
   */
  @SuppressWarnings("rawtypes")
	public Vector listFiles(String directory) throws SftpException{
		  return sftp.ls(directory);
	}
  
    /**
	 * �����������ļ��б�(��ʱ������)
	 */
	public List<File> FileSort(List<File> list) {

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
  
  //�����ļ�
  public boolean copyFile(File file, String destinPath) throws IOException {
	  
	  long startTime=System.currentTimeMillis();
      System.out.println("ִ�д����4����");
	  
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

			
			long endTime=System.currentTimeMillis();
		    System.out.println("��������ʱ�䣺 "+(endTime - startTime)+"ms");
			
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
  

	
  //�ƶ��ļ�
  public void moveFile(String fromPath, String toPath){

      System.out.println("�ƶ��ļ�����·�� " + fromPath + " �ƶ���·�� " + toPath);
      File file = new File(fromPath);
      if (file.isFile()){  
          File toFile=new File(toPath+"\\"+file.getName());  
          if (toFile.exists()){  
             System.out.println("�ļ��Ѵ���");
          }
          else{
              file.renameTo(toFile); 
              System.out.println("�ƶ��ļ��ɹ�");
          } 
      }         
      
   }
  
  
  
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
  
  
   
  //�����ĵ�
  public synchronized void down() throws Exception {
 	 
	    this.sftp = connect();
 	 	System.out.println("���ع���");
 	 	Properties pro=new Properties();
		InputStream in = GJSFTP.class.getClassLoader().getResourceAsStream("properties.properties");
		pro.load(in);

		String out_vmi = pro.getProperty("OUT_VMI"); //��Զ��������·������
	 	String out_ordexe = pro.getProperty("OUT_ORDEXE"); //��Զ��������·������
	 	String out_ack_order = pro.getProperty("OUT_ACK_ORDER"); //��Զ��������·������

	 	String in_vmi = pro.getProperty("IN_VMI"); //���ر���������ļ���·��
	 	String in_ordexe = pro.getProperty("IN_ORDEXE"); //���ر���������ļ���·��
	 	String in_ack_order =pro.getProperty("IN_ACK_ORDER"); //���ر���������ļ���·��
	 	 
	 	batchDownLoadFile(out_vmi, in_vmi, null, ".xml", false);
	 	batchDownLoadFile(out_ordexe, in_ordexe, null, ".xml", false);
	 	batchDownLoadFile(out_ack_order, in_ack_order, null, ".xml", false);

	 	in.close();
	 	
	 	//�����ص��ļ����뵽���ݿ���
 	 	in();
	 	
  }
  
//  �ϴ��ĵ�
  public synchronized void up() throws Exception {
 
      out();
      
      bacthUploadFile(true);
	
  }

  public void in() throws Exception {
	  OraDbUtil od = new OraDbUtil();
	  od.in(); //�������ݿ�
  }
  
  public void out() throws IOException, SQLException{
	
	  OraDbUtil od = new OraDbUtil();
	  od.XmlOutEbs(); //�������ݿ�

  }

  
 /* @throws Exception */
  @SuppressWarnings("static-access")
public synchronized static void main(String[] args) throws Exception
  {
	  GJSFTP su = new GJSFTP();
	 
	  String localPath = su.getLocalPath();
	  new FileSystemXmlApplicationContext(localPath + File.separator + "task-jobs.xml");	
	  
	  su.down(); //�����ļ�
//	  su.up(); //�ϴ��ļ�
   	
  }

}
