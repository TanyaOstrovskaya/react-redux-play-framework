package controllers;
import actors.PointActor;
import models.PointEntry;
import models.UserEntry;
import play.Logger;
import play.db.Database;
import play.db.jpa.JPAApi;
import play.db.jpa.Transactional;
import play.mvc.*;
import akka.actor.*;
import scala.compat.java8.FutureConverters;
import javax.inject.*;
import java.util.concurrent.CompletionStage;
import play.data.DynamicForm;
import play.data.FormFactory;

import static akka.pattern.Patterns.ask;

@Singleton
public class HomeController extends Controller {

    final ActorRef mainActor;
    private Database db;
    private JPAApi jpaApi;

    @Inject FormFactory formFactory;

    @Inject
    public HomeController(ActorSystem system, Database db, JPAApi api) {
        this.mainActor = system.actorOf(PointActor.getProps());
        this.db = db;
        this.jpaApi = api;
    }

    @Transactional
    public CompletionStage<Result> checkPoint (double x, double y, double r) {
        return FutureConverters.toJava(ask(mainActor, new PointEntry(x, y, r), 1000))
                .thenApply(response -> {
                    PointEntry point = (PointEntry)response;
                    addNewPoint(point);
                    System.out.println(point.toString());
                    return ok(Integer.toString(point.getResult()));
                });
    }

    public boolean addNewPoint (PointEntry point) {
        boolean res = jpaApi.withTransaction(entityManager -> {
            entityManager.persist(point);
            return true;
        });
        return res;
    }

    public boolean addNewUser (UserEntry user) {
        boolean res = jpaApi.withTransaction(entityManager -> {
            entityManager.persist(user);
            return true;
        });
        return res;
    }

    public Result signUpNewUser () {
        DynamicForm dynamicForm = formFactory.form().bindFromRequest();
        this.addNewUser(new UserEntry(dynamicForm.get("username"), dynamicForm.get("password"), dynamicForm.get("email")));
        return redirect("/");

    }

    public Result index() {
        return ok(views.html.index.render());
    }

}
