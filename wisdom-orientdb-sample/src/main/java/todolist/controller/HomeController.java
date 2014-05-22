/*
 * Copyright 2014, Technologic Arts Vietnam.
 * All right reserved.
 */

package todolist.controller;

import org.wisdom.api.DefaultController;
import org.wisdom.api.annotations.Controller;
import org.wisdom.api.annotations.Path;
import org.wisdom.api.annotations.Route;
import org.wisdom.api.annotations.View;
import org.wisdom.api.http.Result;
import org.wisdom.api.templates.Template;

import static org.wisdom.api.http.HttpMethod.GET;

/**
 * created: 5/15/14.
 *
 * @author <a href="mailto:jbardin@tech-arts.com">Jonathan M. Bardin</a>
 */
@Controller
@Path("")
public class HomeController extends DefaultController{

    @View("home")
    private Template home;


    @Route(method = GET,uri = "")
    public Result getList(){
        return ok(render(home));
    }
}
