package lu.sfeir.gcloud.demo.jib.controllers;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HelloControllers {

    @GetMapping("/hello/{name}")
    public String Hello(@PathVariable String name) {
        return "Hello " + name;
    }

    @GetMapping("/")
    public String Up() {
        return "I'm Up 😁";
    }

}
