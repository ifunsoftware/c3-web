var tagContainer;
var metaContaier;
var templates={};
var metaDataObject;
var fileNameLabel;
var fidContainer;
var url=" upload.php";
var updUrl;
var idToPost;


function storeData(dataObject){
	sessionStorage.setItem(dataObject.id, dataObject);
}
function popData(id){
	return sessionStorage.getItem(id); 
}



function showError()
{
	console.log("hui");
	metaDataObject = popData(idToPost);
	if (metaDataObject){
		console.log("ne sovsem hui");
		renderForm();
	}else {
		console.log("sovsem hui");
	}
}

function harvest (){
	var resObj ={
		id :'',
		name:'',
		tags:[],
		metadata:{}
	};

	resObj.name = fileNameLabel.html();
	resObj.id = fidContainer.html();
	tagContainer.children('.tag').each(function(i,tg){
		resObj.tags.push( $(tg).data('tag'));
	});

	metaContainer.children('.metaitem').each(function(i,tr){
		key = $(tr).children()[0].innerHTML;
		val = $(tr).children()[1].innerHTML;

		resObj.metadata[key]=val;
	});

	return resObj;

}

function sendData(){

	var dataToPost = harvest();
	console.log(dataToPost);
	storeData(dataToPost);

	$.ajax({
  		type: 'POST',
  		url: updUrl,
  		data: dataToPost,
  		success: handleUploadSuccess,
  		error:showUploadError
  		

	});

}

function handleUploadSuccess ()
{
	console.log ("Succes sending!");
}

function showUploadError(){
	console.log("Try later");
}

function renderForm(){

	fileNameLabel.html(metaDataObject.name);
	fidContainer.html(metaDataObject.id);

	var html='';
	$.each (metaDataObject.tags,function (i,tg){
		html+=templates.tag({"tag":tg});
	});
	tagContainer.html(html);
	html="";
	

	$.each (metaDataObject.metadata,function (key,value){
		html+=templates.meta({"key":key , "value" : value});
	});
	metaContainer.html(html);




	$('#myModal').modal('show');







}





function handleSuccess(data){
	console.log(data);
   metaDataObject=  data;    //jQuery.parseJSON(data);
   
   storeData(metaDataObject);
   renderForm();
}


function startForm(id){
	 idToPost = id;
	$.ajax({
  type: 'POST',
  url: url,
  data: {"id":idToPost},
  success: handleSuccess,
  error:showError,
  dataType: "json"

	});


}




$('document').ready(function() {

	tagContainer = $("#tagContainer");
	metaContainer=$("#metaContaier");
	fileNameLabel=$(".filename");
	fidContainer = $("#fid");

	$("#close").click(function() {

		$('#myModal').modal('hide');
	});

	$(".file").click(function(){
		var id = $(this).data('id');


		startForm(id);


 
	});
	$(".delete").live('click',function(){
		$(this).parents('tr').remove();


	});


	$("#addtag").live("click",function(){
		tg = $('#tagval').val();
		console.log($('#tagval'));
		if ( (!!tg)){
			html = templates.tag({'tag':tg});
			tagContainer.append(html);
		}else {
			console.log("NULL!!!!111111")
		}

		
		$('#tagval').val('');
	});

	$("#addmeta").live("click",function(){
		kk= $('#metakey').val();
		vl= $('#metavalue').val();
		console.log (kk + "		" + vl);
		if ( (!!kk)&&(!!vl)){
			html = templates.meta({'key':kk,'value': vl});
			metaContainer.append(html);
		}else {
			console.log("NULL!!!!111111")
		}
		$('#metakey').val('');
		$('#metavalue').val('');


	});

	$('#upload').click(sendData);


	templates.tag=Handlebars.compile($("#tag-template").html());
	templates.meta=Handlebars.compile($("#meta-template").html());



});
	