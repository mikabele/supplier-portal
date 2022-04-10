import eu.timepit.refined.api.Refined
import eu.timepit.refined.numeric._
import eu.timepit.refined.string._

package object types {
  type UuidStr = String Refined Uuid
  // TODO - fix this type
  type NonEmptyStr      = String Refined MatchesRegex["\\w+"]
  type PhoneStr         = String Refined MatchesRegex["\\+\\d+"]
  type EmailStr         = String Refined MatchesRegex["\\w+@\\w+.\\w+"]
  type NonNegativeFloat = Float Refined NonNegative
  type DateStr          = String Refined MatchesRegex["\\d{4}-\\d{2}-\\d{2}"]
  type UrlStr           = String Refined Url
  type PositiveInt      = Int Refined Positive
}
