<?php

$uploaddir = getcwd().DIRECTORY_SEPARATOR.'upl'.DIRECTORY_SEPARATOR;
$uploadfile = $uploaddir.basename($_FILES['file']['name']);

move_uploaded_file($_FILES['file']['tmp_name'], $uploadfile);

?>