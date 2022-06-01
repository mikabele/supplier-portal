import eu.timepit.refined.api.Refined
import eu.timepit.refined.numeric._
import eu.timepit.refined.string._

package object types {
  type UuidStr          = String Refined Uuid
  type NonEmptyStr      = String Refined MatchesRegex[".*"]
  type PhoneStr         = String Refined MatchesRegex["\\+\\d+"]
  type EmailStr         = String Refined MatchesRegex["\\w+@\\w+.\\w+"]
  type NonNegativeFloat = Float Refined NonNegative
  type DateTimeStr =
    String Refined MatchesRegex["\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}"] // format yyyy-MM-dd HH:mm:ss
  type UrlStr      = String Refined Url
  type PositiveInt = Int Refined Positive
}
