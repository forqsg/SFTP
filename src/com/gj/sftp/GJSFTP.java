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
 * sftp工具类
 * 
 * @author qsg
 * @date 2019-10-09
 * @time 下午1:39:44
 * @version 2.0
 */
public class GJSFTP 
{
  private static Logger log = Logger.getLogger(GJSFTP.class.getName());
 
  private String host;//服务器连接ip
  private String username;//用户名
  private int port;//端口号
  String privateKey;
  
  Session session = null;   
  Channel channel = null;   
  ChannelSftp sftp = null;// sftp操作类  
 
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
   * 通过SFTP连接服务器
 * @throws JSchException 
 * @throws IOException 
   */
  public ChannelSftp connect() throws JSchException, IOException
  {
	  
	  Properties pro=new Properties();
	  InputStream in = GJSFTP.class.getClassLoader().getResourceAsStream("properties.properties");
	  pro.load(in);
		
	    /** 主机 */ 
	  String host = pro.getProperty("host"); 
	    /** 端口 */ 
	  int port = Integer.parseInt(pro.getProperty("port")); 
	    /** 用户名 */ 
	  String username = pro.getProperty("username");
	    /**秘钥地址*/   
	  String privateKey = pro.getProperty("privateKey");

	  JSch jsch = new JSch();   
	  
      //支持密钥的方式登陆， 

    //设置不带口令的密钥  
       jsch.addIdentity(privateKey);  
 
      session = jsch.getSession(username, host, port);     
      session.setConfig("StrictHostKeyChecking", "no"); 
      try {
          session.connect();   
      } catch (Exception e) {
          if (session.isConnected())   
              session.disconnect();   
          log.error("连接服务器失败,请检查主机[" + host + "],端口[" + port   
                  + "],用户名[" + username + "],端口[" + port   
                  + "]是否正确,以上信息正确的情况下请检查网络连接是否正常或者请求被防火墙拒绝.");
          javax.swing.JOptionPane.showMessageDialog(null, "连接服务器失败,请检查主机[" + host + "],端口[" + port   
                  + "],用户名[" + username + "],端口[" + port   
                  + "]是否正确,以上信息正确的情况下请检查网络连接是否正常或者请求被防火墙拒绝.");
      }
      channel = (Channel)session.openChannel("sftp");//创建sftp通信通道  
      try {
          channel.connect();   
      } catch (Exception e) {   
          if (channel.isConnected())   
              channel.disconnect();   
          log.error("连接服务器失败,请检查主机[" + host + "],端口[" + port   
                  + "],用户名[" + username + "],秘钥是否正确,以上信息正确的情况下请检查网络连接是否正常或者请求被防火墙拒绝.");
          javax.swing.JOptionPane.showMessageDialog(null,"连接服务器失败,请检查主机[" + host + "],端口[" + port   
                  + "],用户名[" + username + "],秘钥是否正确,以上信息正确的情况下请检查网络连接是否正常或者请求被防火墙拒绝.");
      }
      sftp = (ChannelSftp) channel;   
      System.out.println(sftp+"连接成功！");
      return sftp;
  }
  
  
  /**
   * 关闭连接
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
   * 批量下载文件
   */
  @SuppressWarnings("rawtypes")
	public List<String> batchDownLoadFile(String remotePath, String localPath,
	      String fileFormat, String fileEndFormat, boolean del) {
	  
	  	File localpath = new File(localPath);
		//如果本地目录不存在,就创建本地目录
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
	        System.out.println("本次处理文件个数不为零,开始下载...文件数量为：" + v.size());
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
	            // 三种情况
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
	        log.info("已成功下载文件，远程地址=" + remotePath
	            + "   本地地址=" + localPath + ",文件数量为"
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
   * 下载单个文件
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
        log.info("远程文件:" + remoteFileName + " 下载成功.");
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
   * 上传单个文件
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
	    
	      System.out.println("文件：" + file+ " 已上传成功，文件大小："+file.length()+"字节");

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
   * 批量上传文件
   */
 
  public void bacthUploadFile(boolean del) throws IOException {
     
	  Properties pro=new Properties();
	  InputStream in = GJSFTP.class.getClassLoader().getResourceAsStream("properties.properties");
	  pro.load(in);
	  String gj_out =  pro.getProperty("GJ_OUT");
	  
	  String in_order =  pro.getProperty("IN_ORDER");//上传至远程主机的路径
	  String in_ack_vmi =  pro.getProperty("IN_ACK_VMI");//上传至远程主机的路径
	  String in_ack_ordexe =  pro.getProperty("IN_ACK_ORDEXE");//上传至远程主机的路径	
//	
//	  String out_order =  pro.getProperty("OUT_ORDER");//本地文件上传的路径
//	  String out_ack_vmi =  pro.getProperty("OUT_ACK_VMI");//本地文件上传的路径
//	  String out_ack_ordexe =  pro.getProperty("OUT_ACK_ORDEXE");//本地文件上传的路径
//	
	  String out_order_bk =  pro.getProperty("OUT_ORDER_BK");//本地备份的路径
	  String out_ack_vmi_bk =  pro.getProperty("OUT_ACK_VMI_BK");//本地备份的路径
	  String out_ack_ordexe_bk =  pro.getProperty("OUT_ACK_ORDEXE_BK");//本地备份的路径
	  
	 try
	    {
	      connect();
	      
	      File file = new File(gj_out);
	      File[] files = file.listFiles(); //取得该路径下的所有文件
	      if(files.length > 0) {
	    	  for (int i = 0; i < files.length; i++)
	          {
	    		  if (files[i].isDirectory()) {
	    			  
	    			  
	    			  if (files[i].getName().equals("OUT_ORDER")) {
	    				  
	    				  File file1 = new File(files[i].getPath());
	    			      File[] files1 = file1.listFiles(); //取得该路径下的所有文件
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
	    			            log.info("上传文件成功:远程地址=" + in_order
	    			                + "   本地地址=" + files[i].getPath() + ", 文件个数： "
	    			                + files1.length);
	    			          }
	    			      }
	    			      else {
	    			        	System.out.println("该文件夹："+files[i].getPath()+"下，无文件信息，请导入文件再上传！！");
	    			        }
		    		  }
		    		  else if (files[i].getName().equals("OUT_ACK_VMI")) {
		    			  File file1 = new File(files[i].getPath());
	    			      File[] files1 = file1.listFiles(); //取得该路径下的所有文件
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
	    			            log.info("上传文件成功:远程地址=" + in_ack_vmi
	    			                + "   本地地址=" + files[i].getPath() + ", 文件个数： "
	    			                + files1.length);
	    			          }
	    			      }
	    			      else {
	    			        	System.out.println("该文件夹："+files[i].getPath()+"下，无文件信息，请导入文件再上传！！");
	    			        }
		    		  }
		    		  else if (files[i].getName().equals("OUT_ACK_ORDEXE")) {
		    			  File file1 = new File(files[i].getPath());
	    			      File[] files1 = file1.listFiles(); //取得该路径下的所有文件
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
	    			            log.info("上传文件成功:远程地址=" + in_ack_ordexe
	    			                + "   本地地址=" + files[i].getPath() + ", 文件个数： "
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
	      	System.out.println("该文件夹："+gj_out+"下，无目录信息，请确定文件目录！！");
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
   * 上传后删除本地文件
   */
  
  public void deletelocal(File file){
		try {
			if(!file.exists()) {
				System.out.println("文件不存在");
			}
			else if (!file.isFile()) {
				System.out.println("请选择文件");
			}
			else{
				file.delete();
				System.out.println("文件："+file+"已删除");
			}
		}catch(Exception e) {
			System.out.println("删除过程中，出现异常");
			e.printStackTrace();
		}

	}
  
  
  
  /**
   * 创建目录
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
          // 建立目录
          sftp.mkdir(filePath.toString());
          // 进入并设置为当前目录
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
   * 判断目录是否存在
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
   * 删除stfp文件
   * @param directory：要删除文件所在目录
   * @param deleteFile：要删除的文件
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
   * 如果目录不存在就创建目录
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
   * 列出目录下的文件
   * 
   * @param directory：要列出的目录
   * @param sftp
   * @return
   * @throws SftpException
   */
  @SuppressWarnings("rawtypes")
	public Vector listFiles(String directory) throws SftpException{
		  return sftp.ls(directory);
	}
  
    /**
	 * 返回排序后的文件列表(按时间排序)
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
  
  //复制文件
  public boolean copyFile(File file, String destinPath) throws IOException {
	  
	  long startTime=System.currentTimeMillis();
      System.out.println("执行代码块4方法");
	  
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

			
			long endTime=System.currentTimeMillis();
		    System.out.println("程序运行时间： "+(endTime - startTime)+"ms");
			
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
  

	
  //移动文件
  public void moveFile(String fromPath, String toPath){

      System.out.println("移动文件：从路径 " + fromPath + " 移动到路径 " + toPath);
      File file = new File(fromPath);
      if (file.isFile()){  
          File toFile=new File(toPath+"\\"+file.getName());  
          if (toFile.exists()){  
             System.out.println("文件已存在");
          }
          else{
              file.renameTo(toFile); 
              System.out.println("移动文件成功");
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
  
  
   
  //下载文档
  public synchronized void down() throws Exception {
 	 
	    this.sftp = connect();
 	 	System.out.println("下载功能");
 	 	Properties pro=new Properties();
		InputStream in = GJSFTP.class.getClassLoader().getResourceAsStream("properties.properties");
		pro.load(in);

		String out_vmi = pro.getProperty("OUT_VMI"); //从远程主机的路径下载
	 	String out_ordexe = pro.getProperty("OUT_ORDEXE"); //从远程主机的路径下载
	 	String out_ack_order = pro.getProperty("OUT_ACK_ORDER"); //从远程主机的路径下载

	 	String in_vmi = pro.getProperty("IN_VMI"); //本地保存服务器文件的路径
	 	String in_ordexe = pro.getProperty("IN_ORDEXE"); //本地保存服务器文件的路径
	 	String in_ack_order =pro.getProperty("IN_ACK_ORDER"); //本地保存服务器文件的路径
	 	 
	 	batchDownLoadFile(out_vmi, in_vmi, null, ".xml", false);
	 	batchDownLoadFile(out_ordexe, in_ordexe, null, ".xml", false);
	 	batchDownLoadFile(out_ack_order, in_ack_order, null, ".xml", false);

	 	in.close();
	 	
	 	//将下载的文件导入到数据库中
 	 	in();
	 	
  }
  
//  上传文档
  public synchronized void up() throws Exception {
 
      out();
      
      bacthUploadFile(true);
	
  }

  public void in() throws Exception {
	  OraDbUtil od = new OraDbUtil();
	  od.in(); //导入数据库
  }
  
  public void out() throws IOException, SQLException{
	
	  OraDbUtil od = new OraDbUtil();
	  od.XmlOutEbs(); //导出数据库

  }

  
 /* @throws Exception */
  @SuppressWarnings("static-access")
public synchronized static void main(String[] args) throws Exception
  {
	  GJSFTP su = new GJSFTP();
	 
	  String localPath = su.getLocalPath();
	  new FileSystemXmlApplicationContext(localPath + File.separator + "task-jobs.xml");	
	  
	  su.down(); //下载文件
//	  su.up(); //上传文件
   	
  }

}
