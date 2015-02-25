package models
import java.text.SimpleDateFormat
import java.util.Date

case class User( var id: String,
				 var birthday: String,
				 var age: Long,
                 var firstName: String,
                 var lastName: String,
                 var active: Boolean)

object JsonFormats {
  import play.api.libs.json.Json

  // Generates Writes and Reads for Feed and User thanks to Json Macros
  implicit val userFormat = Json.format[User]
}