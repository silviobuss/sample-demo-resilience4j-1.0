package io.github.robwin.controller;

import io.github.robwin.service.BusinessService;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value = "/backendB")
public class BackendBController {

    private final BusinessService businessBService;

    public BackendBController(@Qualifier("businessBService")BusinessService businessBService){
        this.businessBService = businessBService;
    }

    @GetMapping("failure")
    public String failure(){
        return businessBService.failure();
    }

    @GetMapping("success")
    public String success(){
        return businessBService.success();
    }

    @GetMapping("ignore")
    public String ignore(){
        return businessBService.ignore();
    }

    @GetMapping("fallback")
    public String failureWithFallback(){
        return businessBService.failureWithFallback();
    }
}
