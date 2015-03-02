package controllers

import scalaz.\/
import play.api._
import play.api.mvc._
import play.api.libs.json._
import play.api.Play.current
import play.api.db.slick.DB

import org.hablapps.meetup.{domain, db, logic}, 
  logic._,
  db._,
  domain._

object Members extends Controller{

  def add(gid: Int): Action[Int] =
    Action(parse.json[Int]) { 
      fromHTTP(gid) andThen 
      join          andThen
      interpreter   andThen
      toHTTP
    }
  
  def interpreter[U]: Store[U] => \/[StoreError, U] = 
    MySQLInterpreter.run[U]
    // MapInterpreter.output[U](MapInterpreter.MapStore())
  
  def fromHTTP(gid: Int): Request[Int] => JoinRequest = 
    request => JoinRequest(None, request.body, gid)


  def toHTTP(response: \/[StoreError, \/[JoinRequest, Member]]): Result =
    response fold(
      error => error match {
        case error@NonExistentEntity(id) => 
          NotFound(s"${error.msg}")
        case error@ConstraintFailed(IsMember(_,_,_)) => 
          Forbidden(s"${error.msg}")
        case error@ConstraintFailed(IsPending(_,_,_)) => 
          Forbidden(s"${error.msg}")
        case error => 
          InternalServerError(error.msg)
      },
      success => success fold(
        joinRequest => 
          Accepted(s"Join request $joinRequest, left pending for futher processing"),
        member => 
          Created(Json.toJson(member)(Json.writes[Member]))
      )
    )

}