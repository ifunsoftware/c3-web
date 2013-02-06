 $(document).ready(function(){
    //show/hide add category
    $("#category_name_1").click(function(){
        $("#category_name_2").toggleClass("category_name_hide category_name_show");

    });
    //show/hide add tags
    //current category
     $(".tagAddButton").live("click",function(){
           $(this).parent().children("form").toggleClass("category_name_hide category_name_show");
           var CatName = $(this).parent().parent().children("h2").text();
           $(this).parent().children("form").children(".inputCateg").val(CatName);
     });
     //for delete, current value tag
     $(".close_orient").live("click",function(){
        var tag = $(this).parent().children("span").text();
        $(this).parent().children(".tagName").val(tag);
        var CatName = $(this).parent().parent().children("h2").text();
        $(this).parent().children(".CatName").val(CatName);
     });
     $(".Add_button").live("click",function(){
        $("#input_container").toggleClass("hide show");
        $("#cont_button_message").toggleClass("hide show");

     });
});