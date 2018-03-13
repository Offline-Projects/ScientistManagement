<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<!-- saved from url=(0016)http://localhost -->
<%@ page language="java" contentType="text/html; charset=UTF-8"
	pageEncoding="UTF-8"%>
<%
    String path = request.getContextPath();
			String basePath = request.getScheme() + "://" + request.getServerName() + ":" + request.getServerPort()
					+ path + "/";
			response.setHeader("Access-Control-Allow-Origin", "*");
%>

<html>
<head>
<base href="<%=basePath%>">
<meta http-equiv="X-UA-compatiable" content="IE=IE7">
<title>全球科学家分布后台管理</title>
<script type="text/javascript" src="./js/jquery.min.js"></script>
<script type="text/javascript" src="./webuploader/webuploader.min.js"></script>
<link rel="stylesheet" type="text/css"
	href="./webuploader/webuploader.css" />
<link rel="stylesheet" type="text/css" href="./css/common.css" />
<script type="text/javascript">
    if($.browser.msie&&($.browser.version == "9.0" || $.browser.version == "8.0")) {
        alert("当前浏览器版本为IE"+$.browser.version + "，请确保已安装Adobe Flash Player，\n并开启浏览器兼容模式，否则上传文件功能无法正常使用！");
    }
    
	//文件上传
	jQuery(function() {
		
		var $ = jQuery, $list = $('#thelist'), $btn = $('#ctlBtn'), state = 'pending', uploader,$removeBtn;
		
		uploader = WebUploader.create({
			// 不压缩image
			resize : false,
			//fileNumLimit:1,
			formData : {
				uid : 123
			},
			// swf文件路径
			swf : './webuploader/Uploader.swf',
			// 文件接收服务端。
			server : './upload',
			//'http://192.168.199.120:8080/FileUpload/upload',
			// 选择文件的按钮。可选。
			// 内部根据当前运行是创建，可能是input元素，也可能是flash.
			withCredentials: true,  // 支持CORS跨域带cookie
			pick : '#picker'
		});
		uploader.on('uploadBeforeSend', function( block, data, header) {
			$.extend({
				"Access-Control-Allow-Origin":"*",
				"Access-Control-Allow-Methods":"*"
				});
		});
		// 当有文件添加进来的时候
		uploader.on('fileQueued', function(file) {
			//$list.text("");
			$list.append('<div id="' + file.id + '" class="item">'
					+ '<a class="info">' + file.name + '</a>'+ '<a class="state">等待上传...</a>'
					+ '<button class="remove" id="'+file.id+'"> 移除' + '</button>'
					 + '</div>');
			$removeBtn = $(".remove");
			$removeBtn.on('click',function(){
	             uploader.removeFile($(this).attr('id'));
	             var s = '#'+$(this).attr('id');
	             $(s).remove();
	         });
		});
		// 文件上传过程中创建进度条实时显示。
		uploader
				.on(
						'uploadProgress',
						function(file, percentage) {
							var $li = $('#' + file.id), $percent = $li
									.find('.progress .progress-bar');
							// 避免重复创建
							if (!$percent.length) {
								$percent = $(
										'<div class="progress progress-striped active">'
												+ '<div class="progress-bar" role="progressbar" style="width: 0%">'
												+ '</div>' + '</div>')
										.appendTo($li).find('.progress-bar');
							}
							$li.find('p.state').text('上传中');
							$percent.css('width', percentage * 100 + '%');
							$('#progress').css('width', percentage * 100 + '%');
							//$percent.css('display','block');
		});
		
		uploader.on('uploadSuccess', function(file, response) {
			$('#' + file.id).find('p.state').text('已上传');
			$('#progress').css('width', 0 + '%');
			alert("上传成功");
			$list.text("");
			history.go(0);
		});
		
		uploader.on('uploadError', function(file, reason) {
			console.log(reason);
			$('#' + file.id).find('p.state').text('上传出错');
			alert("上传出错");
			$list.text("");
            history.go(0);
		});
		
		uploader.on('uploadComplete', function(file) {
			$('#' + file.id).find('.progress').fadeOut();
		});
		
		uploader.on('all', function(type) {
			if (type === 'startUpload') {
				state = 'uploading';
			} else if (type === 'stopUpload') {
				state = 'paused';
			} else if (type === 'uploadFinished') {
				state = 'done';
			}
// 			if (state === 'uploading') {
// 				$btn.text('暂停上传');
// 			} else {
// 				$btn.text('开始上传');
// 			}
		});

		$btn.on('click', function() {
			if (state === 'uploading') {
				uploader.stop();
			} else {
				uploader.upload();
			}
		});
		
		
		
		// 	    $("#picker").on('click', function() {
		// 	          $list.text("")
		// 	          uploader.reset();
		// 	    });
		
// 		.each(function(){
//             $(this).click(function(){
//                  var imgid = $(this).attr("id");
//                  alert(imgid );
//             })
//         });
	});
</script>
<script>
	//$.get()方法  
	function ajaxGet() {
		$("#tip").css('display', 'inline');
		$.get(
		//"http://192.168.199.120:8080/FileUpload/update",//更新地址 
		"./update", function(data) { //回传函数  
			alert(data);
			$("#tip").css('display', 'none');
		}, "text")
	}
</script>
</head>
<body>
	<div class="content tc">
		<img alt="logo" src="./pics/logo.jpg">
	</div>
	<div class="content tc" style="width: 550px;">
		<table border="0">
			<tr>
				<td><div class="mt tl" id="picker">选取上传文件</div></td>
				<td><div class="mt tl">
						<button class="button uploadBtn state-ready" id="ctlBtn">开始上传</button>
					</div></td>
			</tr>

			<tr>
				<td><div id="thelist" class="uploader-list"></div></td>
			</tr>

			<tr>
				<td>
					<div id="progress"
						style="background: #0000FF; height: 10px; width: 0%"></div>
				</td>
			</tr>



			<tr>
				<td>
					<div class="mt tl">
						<a>一键更新数据(过程耗时60秒左右)：</a>
					</div>
				</td>
				<td>
					<div class="mt tl">
						<button class="button" onclick="ajaxGet()">开始更新</button>
					</div>
				</td>
			</tr>
			<tr>
				<td colspan="2">
					<div class="mt tl">
						<a id="tip" style="display: none">服务器正在处理请求，请耐心等待响应</a>
					</div>
				</td>
			</tr>

		</table>
	</div>
</body>
</html>