/**
 * Created with JetBrains WebStorm.
 * User: alex
 * Date: 15.01.13
 * Time: 15:39
 * To change this template use File | Settings | File Templates.
 */
App.uploadModule.Models.fileItem = Backbone.Model.extend({
    defaults:{
        loaded : 0,
        uploadable : false,
        metadata : "",
        status : "Not loaded",
        errortext :"",
        hasError : false
    },
    upload :function(){
        if(this.get('uploadable')){
            var xhr = new XMLHttpRequest();
            try {

                this.set({'hasError': true});
                this.set({'uploadable':false});
                var fd = new FormData();
                var md = {};
                md.filename = this.get("filename");
                md.description = this.get("metadata");
                fd.append('file',this.get('file'));
                fd.append('metadata',md);

                xhr.upload.addEventListener('progress', this.updateProgress, false);
                xhr.onreadystatechange = this.stateChange;
                xhr.open('POST', App.uploadModule.Config.getUrl());
                xhr.setRequestHeader('X-FILE-NAME', this.get('filename'));
                xhr.send(fd);
                this.set({'status' : 'loading'});

            }catch(e){
                App.logger(e);
                this.set({'uploadable':true});
                this.set({'status' : 'connection error'});
                this.set({'hasError': true,'errorext':e.message});
            }
        }

    },
    updateProgress:function(event){
        var percent = parseInt(event.loaded / event.total * 100);
        App.logger(percent);
        this.set({'loaded':percent});
        this.set({'status':percent+"%"});
    },
    stateChange:function(event){
        if(event.target.readyState == 4) {
            if(event.target.status == 200) {
                //dropZone.text('Çàãðóçêà óñïåøíî çàâåðøåíà!');
                App.logger('loaded');
                this.set({'status' : event.target.responseText});

            } else {
                this.set({'uploadable':true});
                App.logger('error loading');
                this.set({'hasError': true,'errorext':event.target.status});
            }

        }

    },


    initialize : function(){
        var file= this.get('file');
        this.set({'filename':file.name});
        this.set({'uploadable':true});
        _.bindAll(this);
    }
});