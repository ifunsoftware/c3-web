package org.aphreet.c3.model

import net.liftweb.mapper._
import net.liftweb.common._
import net.liftweb.http.{SessionVar, S, SHtml}
import xml._
import net.liftweb.http.js.JsCmds.FocusOnLoad
import net.liftweb.util.Helpers._
import net.liftweb.http.S._
import net.liftweb.util.Mailer
import net.liftweb.util.Mailer.Subject
import net.liftweb.sitemap.Loc.LocGroup
import xml.Text
import net.liftweb.util.Mailer.From
import net.liftweb.util.Mailer.To
import net.liftweb.common.Full
import net.liftweb.util.Mailer.BCC

/**
 * The singleton that has methods for accessing the database
 */
object User extends User with MetaMegaProtoUser[User]{

  override def loginMenuLocParams = LocGroup("loginLogoutMenu") :: super.loginMenuLocParams
  override def createUserMenuLocParams = LocGroup("loginLogoutMenu") :: super.createUserMenuLocParams

  def findByEmail(email: String): Box[User] = this.find(By(User.email, email))

  def currentSearchRequests: List[String] = User.currentUser.map(_.searchRequests.get).openOr(Nil)

  def addSearchRequest(req : String) = {
    User.currentUser match {
      case Full(user) => {
        if( req !="" )user.searchRequests.set(req :: user.searchRequests.get.filter(_ != req))
      }
      case _ => {}
    }
  }

  // for stateful redirects on login (depends on where user wanted to go)
  override def logUserIn(who: User, postLogin: () => Nothing) : Nothing = {
    val currentRedirect : Box[String] = loginRedirect
    super.logUserIn(who, () => {
        loginRedirect(currentRedirect)
        postLogin()
    })
  }

  override def dbTableName = "users" // define the DB table name

  // define the order fields will appear in forms and output
  override def fieldOrder = List(id, firstName, lastName, email,
  locale, timezone, password, textArea)

  // comment this line out to require email validations
  override def skipEmailValidation = true

  override def lostPassword = {
      bind("user", lostPasswordXhtml,
        "email" -> SHtml.text("", sendPasswordReset _, ("placeholder" -> S.?("email.placeholder")),("id"->"log inputIcon"),("class"->"username span2")),
        "submit" -> lostPasswordSubmitButton(S.?("lost_password.send")))
  }

  override def lostPasswordXhtml = {
    (<div class="forgot_password_form form-holder login-form well well_login">
        <form action={S.uri} method="POST" class="form">
          <fieldset>
            <legend>{S.?("lost_password.legend")}</legend>
            <label for="username">Email</label>
            <div class="div_text">
              <div class="input-prepend"><span class="add-on"><i class="icon-lock"></i></span><user:email/></div>
            </div>

            <div class="button_div">
              <user:submit />
            </div>

          </fieldset>
        </form>
      </div>)
  }

  // password recovery form
  override def passwordResetXhtml = {
    (<div class="forgot_password_form form-holder login-form well well_login">
      <form action={S.uri} method="POST" class="form">
        <fieldset>
          <legend>{S.?("reset_your_password.legend")}</legend>
          <label for="username">New password</label>
          <div class="div_text">
            <div class="input-prepend"><span class="add-on"><i class="icon-lock"></i></span><user:pwd/></div>
          </div>
          <label for="username">Repeate password</label>
          <div class="div_text">
            <div class="input-prepend"><span class="add-on"><i class="icon-repeat"></i></span><user:pwd/></div>
          </div>
          <div class="button_div">
            <user:submit/>
          </div>
        </fieldset>
    </form>
    </div>)
  }

  override def passwordReset(id: String) =
    findUserByUniqueId(id) match {
      case Full(user) =>
        def finishSet() {
          user.validate match {
            case Nil => S.notice(S.??("password.changed"))
            user.resetUniqueId().save
            logUserIn(user, () => S.redirectTo(homePage))

            case xs => S.error(xs.str)
          }
        }
        bind("user", passwordResetXhtml,
          "pwd" -> SHtml.password_*("",(p: List[String]) =>
            user.setPasswordFromListString(p)),
          "submit" -> resetPasswordSubmitButton(S.?("set.password.button"), finishSet _))
      case _ => S.error(S.??("password.link.invalid")); S.redirectTo(homePage)
    }

  // password recovery email generation
  override def sendPasswordReset(email: String) {
    findUserByUserName(email) match {
      case Full(user) if user.validated_? =>
        user.resetUniqueId().save
        val resetLink = S.hostAndPath+
          passwordResetPath.mkString("/", "/", "/")+urlEncode(user.getUniqueId())

        val email: String = user.getEmail

        Mailer.sendMail(From(emailFrom),Subject(S.?("reset.password.request")),
          (To(user.getEmail) ::
            generateResetEmailBodies(user, resetLink) :::
            (bccEmail.toList.map(BCC(_)))) :_*)

        S.notice(S.?("password.reset.email.sent"))
        S.redirectTo(homePage)


      case Full(user) =>
        sendValidationEmail(user)
        S.notice(S.??("account.validation.resent"))
        S.redirectTo(homePage)

      case _ => S.error(userNameNotFoundString)
    }
  }
  override def passwordResetMailBody(user: TheUserType, resetLink: String): Elem = {
    (<html>
      <head>
        <title>{S.??("reset.password.confirmation")}</title>
      </head>
      <body>
        <p>{S.?("dear")} {user.getFirstName},
          <br/>
          <br/>
          {S.?("reset.password.mail.body.line1")}<br/>
          {S.?("reset.password.mail.body.line2")}<br/>
          {S.?("reset.password.mail.body.line3")}<br/>
          <a href={resetLink}>{resetLink}</a><br/>
          <br/>
          {S.?("reset.password.mail.body.line4")}<br/>
          <br/>
          {S.?("reset.password.mail.end")}
        </p>
      </body>
    </html>)
  }
  override def loginXhtml = {
    (
      <div class="form-holder login-form well well_login">
        <form action={S.uri} method="POST" class="form">
          <fieldset>
            <legend>Existing Users Login</legend>

            <label for="username">Email</label>
            <div class="div_text">
              <div class="input-prepend"><span class="add-on"><i class="icon-envelope"></i></span><user:email/></div>
            </div>
            <label for="password">Password</label>
            <div class="div_text">
              <div class="input-prepend"><span class="add-on"><i class="icon-lock"></i></span><user:password /></div>
            </div>
            <div class="button_div"><input name="rememberme" type="checkbox" id="rememberme" value="forever" />&nbsp;Remember me&nbsp;&nbsp;<user:submit/></div>
            <div class="clear"></div>
            <div align="right">Forgot password?&nbsp;<a href={this.lostPasswordMenuLoc.open_!.loc.createDefaultLink.get}>Click here to reset</a></div>
            <div align="right">New User?&nbsp;<a href={this.createUserMenuLoc.open_!.loc.createDefaultLink.get}>Click here to register</a></div>
            <div class="clear"></div>
          </fieldset>
        </form>
     </div>
    )
  }
  override protected def localForm(user: TheUserType, ignorePassword: Boolean, fields: List[FieldPointerType]): NodeSeq = {
    for {
      pointer <- fields
      field <- computeFieldFromPointer(user, pointer).toList
      if field.show_? && (!ignorePassword || !pointer.isPasswordField_?)
      form <- field.toForm.toList
    } yield <div class="div_text">{form}</div>
  }
  override def signupFields: List[FieldPointerType] = List(firstName,lastName,email,password)
  override def editFields: List[FieldPointerType] = List(firstName,lastName, email)
  override def signupXhtml(user: TheUserType) = {
    (<div class="form-holder signup-form login-form well well_login">
      <form action={S.uri} method="POST" class="form">
        <fieldset>
          <legend>{S.?("signup.legend")}</legend>
            {localForm(user, false, signupFields)}
          <div class="button_div">
            <user:submit/>
          </div>
        </fieldset>
      </form>
    </div>)
  }

  override def editXhtml(user: TheUserType) = {
    ( <div class="form-holder edit-form login-form well well_login">
        <form action={S.uri} method="POST" class="form">
          <fieldset>
            <legend>{S.?("edit.legend")}</legend>
              {localForm(user, true, editFields)}
            <div class="button_div">
            <user:submit/>
              </div>
          </fieldset>
        </form>
      </div>)
  }
  override def changePasswordXhtml = {
    (<div class="form-holder edit-form login-form well well_login">
      <form action={S.uri} method="POST" class="form">
        <fieldset>
          <legend>{S.?("changePassword.legend")}</legend>

          <label for="username">{S.?("old.password")}</label>
          <div class="div_text">
              <div class="input-prepend">
                <span class="add-on"><i class="icon-lock"></i></span><user:old_pwd />
              </div>
          </div>
          <label for="username">{S.?("new.password")}</label>
          <div class="div_text">
            <div class="input-prepend">
              <span class="add-on"><i class="icon-lock"></i></span><user:new_pwd />
            </div>
          </div>
          <label for="username">{S.?("repeatPassword.label")}</label>
            <div class="div_text">
              <div class="input-prepend">
                <span class="add-on"><i class="icon-repeat"></i></span><user:new_pwd />
              </div>
            </div>
          <div class="button_div">
            <user:submit />
          </div>
        </fieldset>
        </form>
      </div>)
  }
  override def changePassword = {
      val user = currentUser.open_! // we can do this because the logged in test has happened
      var oldPassword = ""
      var newPassword: List[String] = Nil

      def testAndSet() {
        if (!user.testPassword(Full(oldPassword))) S.error(S.??("wrong.old.password"))
        else {
          user.setPasswordFromListString(newPassword)
          user.validate match {
            case Nil => user.save; S.notice(S.??("changePassword.changed")); S.redirectTo(homePage)
            case xs => S.error(xs.str)
          }
        }
      }
      bind("user", changePasswordXhtml,
        "old_pwd" -> SHtml.password("", s => oldPassword = s,("class","username span2")),
        "new_pwd" -> SHtml.password_*("", LFuncHolder(s => newPassword = s),("class","username span2")),
        "submit" -> changePasswordSubmitButton(S.?("changePassword.button"), testAndSet _))
      }
  override def screenWrap = Full(
    <lift:surround with="default" at="content">

      <div class="user-form">
          <lift:bind />
      </div>

    </lift:surround>
  )
  override def edit = {
    super.edit ++
    <br/>
    <lift:Menu.item name="ChangePassword">
      Change password
    </lift:Menu.item>.toList
  } : NodeSeq
  override def signup = {
    val theUser: TheUserType = mutateUserOnSignup(createNewUserInstance())
    val theName = signUpPath.mkString("")

    def testSignup() {
      validateSignup(theUser) match {
        case Nil =>
          actionsAfterSignup(theUser, () => {S.notice(S.?("signup.user")); S.redirectTo(homePage)})
        case xs => S.error(xs.str) ;signupFunc(Full(innerSignup _))
      }
    }

    def innerSignup = bind("user",
      signupXhtml(theUser),
      "submit" -> signupSubmitButton(S.?("signup.button"), testSignup _))
    innerSignup
  }
  override def standardSubmitButton(name: String,  func: () => Any = () => {}) = {
    SHtml.submit(name, func,("class","btn btn-primary"))
  }


  override def login = {
    if (S.post_?) {
      S.param("username").
        flatMap(username => findUserByUserName(username)) match {
        case Full(user) if user.validated_? && user.enabled.is &&
          user.testPassword(S.param("password")) => {
          logUserIn(user, () => {
            S.notice(S.??("logged.in"))

            val redir = loginRedirect.is match {
              case Full(url) =>
                loginRedirect(Empty)
                url
              case _ =>
                homePage
            }
            S.redirectTo(redir)
          })
        }

        case Full(user) if !user.validated_? =>
          S.error(S.??("account.validation.error"))

        case _ => S.error(S.??("invalid.credentials"))
      }
    }

    bind("user", loginXhtml,
      "email" -> (FocusOnLoad(<input name="username" type="text" id="log inputIcon" value="" class="username span2" />)),
      "password" -> (<input name="password" type="password" id="pwd inputIcon" class="password span2" />),
      "submit" -> (<input type="submit" name="Submit" value="Login" class="btn btn-primary" />))
  }

}

/**
 * An O-R mapped "User" class that includes first name, last name, password and we add a "Personal Essay" to it
 */
class User extends MegaProtoUser[User] with ManyToMany {
  thisuser =>

  def getSingleton = User // what's the "meta" server

  // define an additional field for a personal essay
  object textArea extends MappedTextarea(this, 2048) {
    override def textareaRows  = 10
    override def textareaCols = 50
    override def displayName = "Personal Essay"
  }

  object groups extends MappedManyToMany(UserGroup, UserGroup.user, UserGroup.group, org.aphreet.c3.model.Group){
    def toForm: NodeSeq = {
      if(!this.toList.isEmpty) {
        {<ul>{
          for(group <- this.toList) yield
          (<li>{Text(group.name.is).toSeq ++ SHtml.checkbox(true, (selected: Boolean) => if(! selected) UserGroup.find(By(UserGroup.group,group),By(UserGroup.user,thisuser)).open_!.delete_!).toSeq}</li>).flatten
         }</ul>}.flatten
      }
      else Text("No groups.")
    }
  }

  object enabled extends MappedBoolean(this){
    override def defaultValue = true
  }

      // this is just a prototype change
  //<div class="input-prepend"><label for="password">{field.displayName}</label>{form}</div>
  override lazy val email: MappedEmail[T] = new MyEmail(this, 128){

    override def _toForm: Box[Elem] =
      fmapFunc({s: List[String] => this.setFromAny(s)}){name =>
        Full( <div class="input-prepend">
                <label for="password">{S.?("email.label")}</label>
                  {appendFieldId(
                    <div class="input-prepend">
                      <span class="add-on"><i class="icon-envelope"></i></span>
                      <input type={formInputType}
                                name={name}
                                class="username span2"
                                placeholder ={S.?("email.placeholder")}
                                value={is match {case null => "" case s => s.toString}}/>
                    </div>)}
              </div>)}

  }

  override lazy val firstName: MappedString[T] = new MyFirstName(this, 32){
    override def _toForm: Box[Elem] =
      fmapFunc({s: List[String] => this.setFromAny(s)}){name =>
        Full(<div class="input-prepend">
          <label for="password">{S.?("firstName.label")}</label>
          {appendFieldId(<div class="input-prepend"><span class="add-on"><i class="icon-user"></i></span><input type={formInputType} maxlength={maxLen.toString}
                                  name={name}
                                  class="username span2"
                                  placeholder ={S.?("firstName.placeholder")}
                                  value={is match {case null => "" case s => s.toString}}/>
          </div>)}
        </div>)}
  }

  override lazy val lastName: MappedString[T] = new MyLastName(this, 32) {
    override def _toForm: Box[Elem] =
      fmapFunc({s: List[String] => this.setFromAny(s)}){name =>
        Full(<div class="input-prepend">
          <label for="password">{S.?("lastName.label")}</label>
          {appendFieldId(<div class="input-prepend"><span class="add-on"><i class="icon-user"></i></span><input type={formInputType} maxlength={maxLen.toString}
                                  name={name}
                                  class="username span2"
                                  placeholder ={S.?("lastName.placeholder")}
                                  value={is match {case null => "" case s => s.toString}}/></div>)}
        </div>)}
  }
  override lazy val password: MappedPassword[T] = new MyPassword(this){
   // <div class="input-prepend"><label for="password">{field.displayName}</label>{form}</div>


    override def _toForm: Box[NodeSeq] = {
      S.fmapFunc({s: List[String] => this.setFromAny(s)}){funcName =>
        Full(<div class="input-prepend">
            <label for="password">{S.?("password.label")}</label>
            {appendFieldId(
            <div class="input-prepend">
              <span class="add-on"><i class="icon-lock"></i></span>
              <input type={formInputType} name={funcName} class="password span2" value={is.toString}/>
            </div>)
            }</div>
          <div class="input-prepend">
            <label for="password">{S.?("repeatPassword.label")}</label>
            <div class="input-prepend">
              <span class="add-on"><i class="icon-repeat"></i></span>
              <input type={formInputType} name={funcName} class="password span2" value={is.toString}/>
            </div>
          </div>)
      }
    }
  }

  object searchRequests extends SessionVar[List[String]] ( "scala" :: "java" :: "performance" :: "c3" :: Nil )

  def createLink: NodeSeq = Text("/users/" + id.is)
}
