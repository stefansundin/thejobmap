<%@ page import="com.google.appengine.api.blobstore.BlobstoreServiceFactory" %>
<%@ page import="com.google.appengine.api.blobstore.BlobstoreService" %>
<%
    BlobstoreService blobstoreService = BlobstoreServiceFactory.getBlobstoreService();
%>
<html>
<body style="margin:0;">
<form name="cv" enctype="multipart/form-data" action="<%= blobstoreService.createUploadUrl("/rest/user/cv") %>" method="POST">
<input type="hidden" name="MAX_FILE_SIZE" value="100000" />
<input name="myFile" type="file" onchange="document.cv.submit();" />
<!--<input type="submit" value="Upload File" />-->
</form>
</body>
</html>