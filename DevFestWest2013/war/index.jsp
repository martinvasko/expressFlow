<!DOCTYPE html>
<%@page import="java.security.Security"%>
<%@page import="java.security.Provider"%>
<%@page import="com.google.appengine.api.datastore.KeyFactory"%>
<%@page import="java.util.Date"%>
<%@page import="com.google.appengine.api.blobstore.BlobstoreServiceFactory"%>
<%@page import="com.google.appengine.api.blobstore.BlobstoreService"%>
<%@ page language="java" contentType="text/html; charset=utf-8"
    pageEncoding="UTF-8"%>
<html>
<%
	String dev_session = KeyFactory.createKeyString("Session",new Date().getTime());
	BlobstoreService blobService = BlobstoreServiceFactory.getBlobstoreService();
	String uploadUrl = blobService.createUploadUrl("/upload");
	String uploadGCSUrl = blobService.createUploadUrl("/upload-gcs");
	Provider[]	providers = Security.getProviders();
%>
<head>
<title>Drop it like it's hot</title>
	<script src="//ajax.googleapis.com/ajax/libs/jquery/1.10.2/jquery.min.js"></script>
	<script src="/js/jquery.filedrop.js"></script>
	<script type="text/javascript">
	$(document).ready(function(){
		var uploadStarted;
		
		// Drop key handler for dropPane
		var droppane = $('#dropPane');
		droppane.filedrop({
			paramname:'file',
			data: {	
				dev_session: $('#dev_session').text()
			},
			allowedfiletypes:[],
			maxfiles: 1,
			maxfilesize: 5,
			dragOver: function(){
				droppane.css("background-color","lightgreen");
			},
			dragLeave: function(){
				droppane.css("background-color", "#F6F6F6");
			},
			url: '<%= uploadUrl %>',
			uploadFinished: function(i,file,response){
				var now = new Date().getTime();
				var time = now - uploadStarted;
				droppane.text("Processed in " + time + " ms.");
			},
			uploadStarted:function(i, file, len){
				uploadStarted = new Date().getTime();
			},
			progressUpdated: function(i, file, p){
				droppane.text("Uploading (" + p + "%)" );
			}
		});
		
		// Drop key handler for dropPane
		var droppanegcs = $('#dropPaneGCS');
		droppanegcs.filedrop({
			paramname:'file',
			data: {	
				dev_session: $('#dev_session').text(),
				file_name: $('#file_name').text()
			},
			allowedfiletypes:[],
			maxfiles: 1,
			maxfilesize: 5,
			dragOver: function(){
				droppanegcs.css("background-color","lightgreen");
			},
			dragLeave: function(){
				droppanegcs.css("background-color", "#F6F6F6");
			},
			url: '/upload-gcs',
			uploadFinished: function(i,file,response){
				var now = new Date().getTime();
				var time = now - uploadStarted;
				droppanegcs.text("Processed in " + time + " ms.");
			},
			uploadStarted:function(i, file, len){
				uploadStarted = new Date().getTime();
			},
			progressUpdated: function(i, file, p){
				droppanegcs.text("Uploading (" + p + "%)" );
			},
			beforeSend: function(file, i, done){
				$('#file_name').text(file.name);
				done();
			}
		});
		
	});
	</script>
</head>
<body>
	<div id="dev_session" style="display: none;"><%= dev_session %></div>
	<div id="file_name" style="display: none;"></div>
	<div id="dropPane" style="width: 400px; height: 100px; text-align: center; padding: 60px; background-color: #F6F6F6; border: 1px dashed #666; border-radius: 6px; margin-bottom: 50px; margin: 0px auto;">
	Drop your file via Blobstore cache
	</div>
	<div id="dropPaneGCS" style="width: 400px; height: 100px; text-align: center; padding: 60px; background-color: #F6F6F6; border: 1px dashed #666; border-radius: 6px; margin-bottom: 50px; margin: 0px auto;">
	Drop your file directly to Google Cloud storage
	</div>
	<div style="width: 400px; text-align: center; padding: 60px; border: 1px dashed #666; border-radius: 6px; margin-bottom: 50px; margin: 0px auto;">
	<h3>Available security providers on Google App Engine:</h3>
	<table style="text-align: center;">
	<% if(providers.length > 0){ 
	    for (int i = 0; i != providers.length; i++)
	    { %>
	    <tr>
	    	<td>
	        <h4>Name: <%= providers[i].getName() %></h4>
			</td>
			<td>
			<h4>Version: <%= providers[i].getVersion() %></h4>
			</td>
		</tr>
		<tr>
			<td>
			Algorithms:
			<table style="text-align: center;">
			<% 	Provider provider = providers[i];
			for (Provider.Service s : provider.getServices()) { %>
				<tr>
					<td><%= s.getAlgorithm() %></td>
				</tr>
			<% }%>
			</table>
			</td>
			<td>
			</td>
		</tr>
	<%  }
	%>
	<% } %>
	</table>
	</div>
	<p style="text-align: center; font-size:small;">Powered by <a href="https://github.com/weixiyen/jquery-filedrop" target="_blank">jquery filedrop plugin</a></p>
	<p style="text-align: center; font-size:small;">Part of <a href="https://github.com/martinvasko/expressFlow" target="_blank"><img src="img/expressFlow.png" alt="expressFlow" /></a></p>
</body>