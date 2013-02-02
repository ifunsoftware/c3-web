/**
 * Created with JetBrains WebStorm.
 * User: alex
 * Date: 15.01.13
 * Time: 15:14
 * To change this template use File | Settings | File Templates.
 */
App.uploadModule.Views.Popup = Backbone.View.extend({
   // tagName:'#upload_form',
    show : function(){

        App.logger(this.$el);
       // this.$el.show();   //modal('show');
       this.$el.modal('show');
    },
    hide : function(){
        this.$el.modal('hide');
        this.collection.reset();

    },

    initialize: function(){
        _.bindAll(this);

        this.setElement($('#upload_form'));
        this.collection = new App.uploadModule.Collections.fileList();
        this.dropzone= jQuery('#dropzone');

        this.inpt = $('.input')[0];
        this.collection.on("add",this.addFileView,this);
        this.dr = this.dropzone[0];


        /***
         * <говнокод>
         *
         * ***********************************************
         var $this = this;



        this.dr.ondrop =function(e){
            e.preventDefault();
            var files = e.dataTransfer.files;
            console.log(files);
            return false;
        };
        App.logger(this.dropzone);
        App.logger(this.dr);
        */

       // this.dr.ondragover = function () { /*this.className = 'hover'; */return false;};
        //this.dr.ondragend = function () { /*this.className = '';*/ return false; };


        /***
         * </говнокод>
         *
         *
         * ************************************************/
    },
    events :{
      "click #close" : "hide",
      "click #upload": "upload",
      "drop #dropzone" :"getDroppedFiles",
        "dragover #dropzone" :"dragOver",
        "dragend #dropzone" :"dragEnd",
       "change .input" : "getInpuFiles"
    },

    dragOver : function(e){
        e.preventDefault();
        //App.logger(e,"dragOver");
        //this.dropzone.addClass('hover');
        return false;
    },
    dragEnd : function(e){
        e.preventDefault();
        //App.logger(e,"dragEnd");
        //this.dropzone.removeClass('hover');
        return false;
    },

    upload :function(){
        App.uploadModule.Vent.trigger("upload");

    },
    getInpuFiles : function(e){
        e.preventDefault();
       // this.dropzone.removeClass('hover');
        App.logger(e);

        var files = [];

        files= this.inpt.files;
        this.addFiles(files);
    },
    getDroppedFiles : function(e){
        e.preventDefault();
        var files = e.originalEvent.dataTransfer.files;
        App.logger(files);
        App.logger(e);
        this.addFiles(files);
        return false;
    },
    addFiles : function(files){
        var $that =this;
        App.logger(files);
        $.each(files,function(i,file){
            App.logger(file);
            var fname= file.name;
            if (!$that.collection.find(function(fl){return fl.get('filename')==fname})){
                $that.collection.add({'file':file});
            }

        });

    },
    addFileView : function(fileItem){
        var fileView = new App.uploadModule.Views.File({model : fileItem});
        this.dropzone.append(fileView.render().el);
    }
});

App.uploadModule.Views.File = Backbone.View.extend({
    tagName:'p',
    class : 'filecontainer',
    events :{
        "click .removebutton" : "destroy",
        "click .lineContainer" : "toggleArea",
        "change .area": "updateMeta"
    },
    template :_.template($('#file-template').html()),
    render:function(){
        var html = this.template(this.model.toJSON());
        this.$el.html(html);
        this.area =this.$el.children('.area').hide();
        this.$el.addClass('filecontainer');
        return this;
    },
    toggleArea: function(){
        App.logger(this.area);
        $(this.area).slideToggle();
    },
    initialize : function(){
        _.bindAll(this);
       // this.model.on('change : loaded',this.render,this);
        this.model.on('change : status',this.render,this);

        this.model.on('destroy',this.remove,this);
        App.uploadModule.Vent.on ("upload",this.uploadFile,this);

    },
    updateMeta:function(){
        App.logger(this.area[0]);
        var meta = this.area[0].value;
        this.model.set({metadata:meta});
        //this.area[0].value = meta;
    },

    destroy:function(){
        App.logger(this.model);
        this.model.destroy();
        App.logger("destroyed");
    },
    uploadFile :function(){
        this.model.upload();
    }


});