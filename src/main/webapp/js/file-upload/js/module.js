/**
 * Created with JetBrains WebStorm.
 * User: alex
 * Date: 15.01.13
 * Time: 15:52
 * To change this template use File | Settings | File Templates.
 */

App.uploadModule = {
    Collections:{},
    Models:{},
    Views:{},
    Config:{
        uploadUrl:"/upload/file/groups",
        popupTemplate: ""   ,
        getUrl:function(){
            var relUrl = window.location.pathname.split("groups")[1];
                    if(relUrl == "") relUrl = "/";
                    return this.uploadUrl + relUrl;
        }
    }
};



App.uploadModule.init =function(){
  App.logger("starting init");
  this.Vent = _.extend({},Backbone.Events);
  this.popup = new App.uploadModule.Views.Popup();
  $('#upload_form_button').click(this.popup.show);
    App.logger("end init");
};


$('document').ready(function(){
    App.uploadModule.init();
} );