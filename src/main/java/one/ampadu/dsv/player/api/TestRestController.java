package one.ampadu.dsv.player.api;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TestRestController {

    //TODO remove this todo!
    @GetMapping("/test")
    public String test() {
        return "this is just a test :)";
    }

}
