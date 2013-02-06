$('document').ready(function() {

	var popup = $(".popup");
	var dropZone = $(".dropzone");
	var fileItemList = [];
	var uploadUrl = "upload.php";
	var input = $(".input");

/*
	$("#pressme").click(function() {

		$('#myModal').modal('toggle')
	});
*/
	$("#close").click(function() {

		$('#myModal').modal('hide')
	});


	input.live('change', function() {

		console.log(this.files);
		addFiles(this.files);

	});


	dropZone[0].ondrop = function(event) {
		event.preventDefault();
		var files = [];
		files = event.dataTransfer.files;


		/*
  files.forEach(function(fl) 
  {

  console.log(fl);
   fileItemList.add(new fileItem(fl));
  });
*/
		/*
for(var i = 0, l = files.length; i < l; i++) {
	var fl = files[i];
    
    var Id = window.btoa(fl.name+Math.random()).substr(0,8);
    //fileItemList.push(new fileItem(files[i]));
    fileItemList[Id]= new fileItem(fl,Id);
}

*/


		addFiles(files);


	};


	function fileItem(fl, ID) {
		var file = fl;
		console.log("construct");
		console.log(file);

		var fileName = file.name;
		var alive = true;
		var itemId = ID;


		var xhr = new XMLHttpRequest();
		//window.btoa(fileName+Math.random()).substr(0,8);
		var upload = function() {

				console.log('startupload');

				try {

					alive = false;
					console.log(file);
					var fd = new FormData();
					fd.append("file", file);
					fd.append("metadata", view.metadata);

					//var xhr = new XMLHttpRequest();
					xhr.upload.addEventListener('progress', uploadProgress, false);
					xhr.onreadystatechange = stateChange;
					xhr.open('POST', uploadUrl);
					xhr.setRequestHeader('X-FILE-NAME', fileName);
					xhr.send(fd);
				} catch(e) {
					view.updatebar('Ïðîèçîøëà îøèáêà!');
					alive = true;

				}
			}

		var stateChange = function(event) {
				if(event.target.readyState == 4) {
					if(event.target.status == 200) {
						//dropZone.text('Çàãðóçêà óñïåøíî çàâåðøåíà!');
						view.updatebar('Ok');

					} else {
						alive = true;
						view.updatebar('Ïðîèçîøëà îøèáêà!');
					}

				}


			};
		var uploadProgress = function(event) {
				var percent = parseInt(event.loaded / event.total * 100);
				view.updatebar(percent);
				console.log(percent);

			};


		var view = {
			metadata: "",
			id: "",
			name: "",

			container: {
				tpl: "<p class='filecontainer' id = '{{id}}'>file :</p>",
				instance: ""
			},
			textBox: {
				tpl: "<textarea hidden='true' class ='area'></textarea>",
				instance: ""
			},
			lineContainer: {
				tpl: "<div class = 'lineContainer'>{{name}}</div>",
				instance: ""

			},
			progressBar: {
				tpl: "<div class = 'barcontainer'><div class= 'bartext' >0%</div><div class='bar'></div></div>",
				barcontainer: "",
				bartext: ""

			},
			removebutton: {
				tpl: "<div class = 'removebutton'><i class='icon-trash icon-black'></i></div>",
				instatnce: ""
			},
			init: function() {
				var rootObject = this;
				this.id = itemId;
				
				if (fileName.length>40)
				{
					this.name=fileName.substr(0,25)+"...";	
				}else{
					this.name = fileName;
				}



				
				var str = '#' + this.id;


				dropZone.append($(this.container.tpl.replace("{{id}}", this.id)));


				this.container.instance = $(str);
				console.log(this.container);

				this.render();

				this.textBox.instance = this.container.instance.children('textarea');
				this.lineContainer.instance = this.container.instance.children('.lineContainer');
				this.removebutton.instance = this.container.instance.children('.removebutton');

				this.progressBar.barcontainer = this.container.instance.children('.barcontainer');
				this.progressBar.bar = this.progressBar.barcontainer.children('.bar');
				this.progressBar.bartext = this.progressBar.barcontainer.children('.bartext');


				this.removebutton.instance.on("click", function() {

					console.log(fileItemList[rootObject.id]);

					destroy();

					delete fileItemList[rootObject.id];
					console.log(fileItemList[rootObject.id]);


				});


				this.lineContainer.instance.on("click", function() {

					rootObject.textBox.instance.slideToggle();

				});
				this.textBox.instance.on("change", function() {

					rootObject.metadata = this.value;
					console.log(this.value);
				})

			},
			render: function() {
				var html = "";//это грязный хак 


				html += this.lineContainer.tpl.replace("{{name}}", this.name);
			
				html += this.removebutton.tpl;

				html += this.progressBar.tpl;
				html += this.textBox.tpl;
				html+="</br>";

				this.container.instance.html(html);

			},
			updatebar: function(percent) {

				if(isNumber(percent)) {
					this.progressBar.bar.css('width', percent + "%");
					this.progressBar.bartext.text(percent + "%");
				} else {
					this.progressBar.bartext.text(percent);
				}

			},
			destroy: function() {
				this.container.instance.remove();
			}
		};


		var destroy = function() {
				view.destroy();
				alive = false;
				xhr.abort();
				return null;
			};


		view.init();

		$("#upload").click(function() {
			if(alive) {
				upload();
			}
		});


	}

	function addFiles(fls) {

		for(var i = 0, l = fls.length; i < l; i++) {
			var fl = fls[i];

			var Id = hash(fl.name) //TODO: russian encoding fix
			//fileItemList.push(new fileItem(files[i]));
			if(!fileItemList[Id]) {

				fileItemList[Id] = new fileItem(fl, Id);
			}
		}

	}


});

function isNumber(o) {
	return !isNaN(o - 0);
}

function hash(str) {
	var hash = 0;
	var str = String(str);
	if(str.length == 0) return hash;
	for(i = 0; i < str.length; i++) {
		char = str.charCodeAt(i);
		hash = ((hash << 5) - hash) + char;
		hash = hash & hash; // Convert to 32bit integer
	}
	return hash;
}