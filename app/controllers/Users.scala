package controllers

import play.modules.reactivemongo.MongoController
import play.modules.reactivemongo.json.collection.JSONCollection
import scala.concurrent.Future
import reactivemongo.api.Cursor
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import org.slf4j.{LoggerFactory, Logger}
import javax.inject.Singleton
import play.api.mvc._
import play.api.libs.json._
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * The Users controllers encapsulates the Rest endpoints and the interaction with the MongoDB, via ReactiveMongo
 * play plugin. This provides a non-blocking driver for mongoDB as well as some useful additions for handling JSon.
 * @see https://github.com/ReactiveMongo/Play-ReactiveMongo
 */
@Singleton
class Users extends Controller with MongoController {

  private final val millisInAYear = 31536000000L
  private final val logger: Logger = LoggerFactory.getLogger(classOf[Users])

  /*
   * Get a JSONCollection (a Collection implementation that is designed to work
   * with JsObject, Reads and Writes.)
   * Note that the `collection` is not a `val`, but a `def`. We do _not_ store
   * the collection reference to avoid potential problems in development with
   * Play hot-reloading.
   */
  def collection: JSONCollection = db.collection[JSONCollection]("users")

  // ------------------------------------------ //
  // Using case classes + Json Writes and Reads //
  // ------------------------------------------ //

  import models._
  import models.JsonFormats._

  def createUser = Action.async(parse.json) {
    request =>
    /*
     * request.body is a JsValue.
     * There is an implicit Writes that turns this JsValue as a JsObject,
     * so you can call insert() with this JsValue.
     * (insert() takes a JsObject as parameter, or anything that can be
     * turned into a JsObject using a Writes.)
     */
      request.body.validate[User].map {
        user =>
        // `user` is an instance of the case class `models.User`
        // val formatter = DateTimeFormat.forPattern("yyyy-mm-dd'T'hh:mm:ss.SSS'Z'")
        // val birthdate = formatter.parseDateTime(user.birthday)
        // user.age = (birthdate to DateTime.now).millis / millisInAYear
        user.age = calcalateAgeFromBirthday(user.birthday)

          collection.insert(user).map {
            lastError =>
              logger.debug(s"Successfully inserted with LastError: $lastError")
              Created(s"User Created")
          }
      }.getOrElse(Future.successful(BadRequest("invalid json")))
  }

  def updateUser(id: String) = Action.async(parse.json) {
    request =>
      request.body.validate[User].map {
        user =>
          // find our user by first name and last name
          user.age = calcalateAgeFromBirthday(user.birthday)

          val nameSelector = Json.obj("id" -> id)
          collection.update(nameSelector, user).map {
            lastError =>
              logger.debug(s"Successfully updated with LastError: $lastError")
              Created(s"User Updated")
          }
      }.getOrElse(Future.successful(BadRequest("invalid json")))
  }

  def findUsers = Action.async {
    // let's do our query
    val cursor: Cursor[User] = collection.
      // find all
      find(Json.obj("active" -> true)).
      // sort them by creation date
      sort(Json.obj("created" -> -1)).
      // perform the query and get a cursor of JsObject
      cursor[User]

    // gather all the JsObjects in a list
    val futureUsersList: Future[List[User]] = cursor.collect[List]()

    // transform the list into a JsArray
    val futurePersonsJsonArray: Future[JsArray] = futureUsersList.map { users =>
      Json.arr(users)
    }
    // everything's ok! Let's reply with the array
    futurePersonsJsonArray.map {
      users =>
        Ok(users(0))
    }
  }
  
  def deleteUser(id: String) = Action {
    val nameSelector = Json.obj("id" -> id)
    collection.remove(nameSelector, firstMatchOnly = true)
    Ok("Delete Request was made")
  }

  def calcalateAgeFromBirthday(birthday: String) : Long = {

   val formatter = new SimpleDateFormat("yyyy-mm-dd'T'hh:mm:ss.SSS'Z'")
   val birthDateTime = formatter.parse(birthday).getTime()
   var currentDateTime = new Date().getTime()
   if (currentDateTime > birthDateTime)
    return (currentDateTime - birthDateTime)/millisInAYear
   else
    throw new IllegalArgumentException("Birthday cannot be in the future")
  }
 

}
