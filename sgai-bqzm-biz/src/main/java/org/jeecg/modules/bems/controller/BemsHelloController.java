package org.jeecg.modules.bems.controller;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.jeecg.modules.bems.entity.BemsHelloEntity;
import org.jeecg.modules.bems.service.IBemsHelloService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import lombok.extern.slf4j.Slf4j;

@Api(tags = "bems示例")
@RestController
@RequestMapping("/bems")
@Slf4j
public class BemsHelloController {

	@Autowired
	private IBemsHelloService jeecgHelloService;

	@ApiOperation(value = "hello", notes = "对外服务接口")
	@GetMapping(value = "/hello")
	public String sayHello() {
		log.info(" ---我被调用了--- ");
		return jeecgHelloService.hello();
	}

}
