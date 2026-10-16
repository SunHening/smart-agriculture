package com.smart.agriculture.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Map;

@RestController
@RequestMapping("/api/sam/session")
public class SamSessionController {

    @Value("${sam-geo.service-url:http://localhost:5000}")
    private String samServiceUrl;

    private final RestTemplate restTemplate = new RestTemplate();


    /**
     * 创建 SAM Session
     */
    @PostMapping(
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public ResponseEntity<?> createSession(
            @RequestParam("image") MultipartFile image
    ) throws IOException {


        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);


        org.springframework.util.LinkedMultiValueMap<String,Object> body =
                new org.springframework.util.LinkedMultiValueMap<>();


        body.add(
                "image",
                new MultipartFileResource(image)
        );


        HttpEntity<Object> request =
                new HttpEntity<>(body, headers);


        ResponseEntity<String> response = restTemplate.postForEntity(
                samServiceUrl + "/sam/sessions",
                request,
                String.class
        );

        return ResponseEntity
                .status(response.getStatusCode())
                .contentType(MediaType.APPLICATION_JSON)
                .body(response.getBody());
    }



    /**
     * 查询 Session
     */
    @GetMapping("/{id}")
    public ResponseEntity<?> getSession(
            @PathVariable String id
    ){

        return restTemplate.getForEntity(
                samServiceUrl + "/sam/sessions/" + id,
                Object.class
        );
    }



    /**
     * 点预测
     * Python 返回 PNG
     */
    @PostMapping(
            value="/{id}/prediction",
            consumes = MediaType.APPLICATION_JSON_VALUE
    )
    public void prediction(
            @PathVariable String id,
            @RequestBody Map<String,Object> body,
            HttpServletResponse response
    ) throws IOException {


        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);


        HttpEntity<Map<String,Object>> request =
                new HttpEntity<>(body,headers);


        ResponseEntity<byte[]> result =
                restTemplate.exchange(
                        samServiceUrl +
                                "/sam/sessions/" +
                                id +
                                "/predictions",
                        HttpMethod.POST,
                        request,
                        byte[].class
                );


        response.setContentType("image/png");

        // 转发 Python SAM 返回的置信度
        String samScore = result.getHeaders().getFirst("X-SAM-Score");

        if (samScore != null) {
        response.setHeader("X-SAM-Score", samScore);
        }

        response.getOutputStream()
                .write(result.getBody());
        }


    /**
     * Box 预测
     * Python 返回 PNG
     */
    @PostMapping(
            value="/{id}/box",
            consumes = MediaType.APPLICATION_JSON_VALUE
    )
    public void box(
            @PathVariable String id,
            @RequestBody Map<String,Object> body,
            HttpServletResponse response
    ) throws IOException {

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<Map<String,Object>> request =
                new HttpEntity<>(body, headers);

        ResponseEntity<byte[]> result =
                restTemplate.exchange(
                        samServiceUrl +
                                "/sam/sessions/" +
                                id +
                                "/box",
                        HttpMethod.POST,
                        request,
                        byte[].class
                );

        response.setContentType("image/png");

        // 转发 Python SAM 返回的置信度
        String samScore = result.getHeaders().getFirst("X-SAM-Score");

        if (samScore != null) {
        response.setHeader("X-SAM-Score", samScore);
        }

        response.getOutputStream()
                .write(result.getBody());
    }


    /**
     * 删除 Session
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(
            @PathVariable String id
    ){

        return restTemplate.exchange(
                samServiceUrl +
                        "/sam/sessions/" +
                        id,
                HttpMethod.DELETE,
                null,
                Object.class
        );
    }



    /**
     * Multipart 转换
     */
    static class MultipartFileResource
            extends org.springframework.core.io.InputStreamResource {


        private final String filename;


        MultipartFileResource(
                MultipartFile file
        ) throws IOException {

            super(file.getInputStream());

            this.filename = file.getOriginalFilename();
        }


        @Override
        public String getFilename(){
            return filename;
        }


        @Override
        public long contentLength(){
            return -1;
        }
    }
}
