package controller;

import dao.PictureDAO;
import domain.Picture;
import domain.User;
import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.sql.Timestamp;
import java.util.Date;
import java.util.List;
import java.util.UUID;


@WebServlet(name = "controller.ModifyServlet", value = "/modify")
public class ModifyServlet extends HttpServlet {


    public void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.setCharacterEncoding("UTF-8");
        response.setCharacterEncoding("UTF-8");
        User user=(User)request.getSession().getAttribute("user");
        System.out.println(request.getQueryString());



        //创建一个“硬盘文件条目工厂”对象
        DiskFileItemFactory factory = new DiskFileItemFactory();
        //设置阈值，设置JVM一次能够处理的文件大小（默认吞吐量是10KB）
        factory.setSizeThreshold(DiskFileItemFactory.DEFAULT_SIZE_THRESHOLD);
        //设置临时文件的存储位置（文件大小大于吞吐量的话就必须设置这个值，比如文件大小：1GB ，一次吞吐量：1MB）
        factory.setRepository(new File(request.getSession().getServletContext().getRealPath("source/tmp")));


        //创建核心对象
        ServletFileUpload fileUpload = new ServletFileUpload(factory);
        //设置最大可支持的文件大小（10MB）
        fileUpload.setFileSizeMax(1024*1024*10);
        //设置转换时使用的字符集
        //fileUpload.setHeaderEncoding("UTF-8");

        Picture uploadPic=new Picture();
        PictureDAO pictureDAO=new PictureDAO();

        String imageId=request.getParameter("imageId");
        String isFileEmpty=request.getParameter("picIsEmpty");
        String sql="SELECT imageId,path,title,description,content,username author,uid authorId from travelimage where imageid=?";
        Picture oldPic=pictureDAO.getInstance(sql,imageId);


        if (ServletFileUpload.isMultipartContent(request)) {
            try {
                //解析请求
                List<FileItem> fileItems = fileUpload.parseRequest(request);
                for (FileItem fileItem : fileItems) {
                    if(fileItem.isFormField()) {//判断该FileItem为一个普通的form元素
                        //获取字段名
                        String fieldName = fileItem.getFieldName();
                        //获取字段值，并解决乱码
                        String fieldValue = fileItem.getString("UTF-8");
                        if(fieldName.equals("upload_pic_title"))
                            uploadPic.setTitle(fieldValue);
                        if(fieldName.equals("upload_pic_description"))
                            uploadPic.setDescription(fieldValue);
                        if(fieldName.equals("upload_pic_country")) {
                            uploadPic.setCountry(fieldValue);
                            sql="select geocountries.ISO from geocountries where geocountries.CountryName=?";
                            String countryCode=pictureDAO.getValue(sql,fieldValue);
                            uploadPic.setCountryCode(countryCode);
                        }

                        if(fieldName.equals("upload_pic_city")) {
                            uploadPic.setCity(fieldValue);
                            sql="select geocities.GeoNameID from geocities where geocities.AsciiName=?";
                            Integer cityCode=pictureDAO.getValue(sql,fieldValue);
                            uploadPic.setCityCode(cityCode);
                        }
                        if(fieldName.equals("upload_pic_theme"))
                            uploadPic.setContent(fieldValue);

                        System.out.println(fieldName + ":" + fieldValue);
                    }else {//判断该FileItem为一个文件
                        //获取文件名
                        String fileName = oldPic.getPath();
                        //由于用户的图片的文件名没什么用，为了避免重复这里就用uuid设置防止重复
                        System.out.println("fileName=" + fileName);
                        //获取文件大小
                        long fileSize = fileItem.getSize();
                        System.out.println("fileSize=" + fileSize);
                        if(fileSize!=0) {
                            InputStream inputStream = fileItem.getInputStream();
                            //String path="/Users/yilingzhao/Desktop/卓越软件开发/pj/web/source/upfile/";
                            String path = request.getSession().getServletContext().getRealPath("source/upfile/");
                            //为了实时显示还是需要写到out里的对应文件夹中
                            //之所以source中会有且之所以刷新会有，这只不过是从部署目录下同步过来
                            System.out.println(path);
                            OutputStream out = new FileOutputStream(path + fileName);
                            byte[] buffer = new byte[1024];
                            int len = 0;
                            while ((len = inputStream.read(buffer)) != -1) {
                                out.write(buffer, 0, len);
                                out.flush();
                            }
                            inputStream.close();
                            out.close();
                        }
                        uploadPic.setPath(fileName);
                        uploadPic.setAuthor(user.getUsername());
                        uploadPic.setAuthorId(user.getId());
                        uploadPic.setUploadDate(new Timestamp(new Date().getTime()));
                        //favoredNum 默认为0
                    }

                }
                sql="UPDATE `travelimage` SET `Title`=?,`Description`=?,`Content`=?,`Country`=?,`City`=?,`CityCode`=?,`CountryCodeISO`=?," +
                        "`UploadDate`=? WHERE imageid=?";
                pictureDAO.update(sql,uploadPic.getTitle(),uploadPic.getDescription(),uploadPic.getContent(),uploadPic.getCountry(),uploadPic.getCity()
                        ,uploadPic.getCityCode(),uploadPic.getCountryCode(),uploadPic.getUploadDate(),imageId);

                response.sendRedirect("mypics");
            } catch (FileUploadException e) {
                e.printStackTrace();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

}













