package com.api.media.controller;

import org.springframework.web.bind.annotation.*;

import com.api.annotation.RequirePermission;
 
@RestController
@RequestMapping("/api/media")
public class MediaController {
 
    @RequirePermission("media:read")
    @GetMapping("/{id}")
    public String getMediaItem(@PathVariable Long id) {
        return "media item " + id;
    }
 
    @RequirePermission("media:write")
    @PutMapping("/{id}")
    public String updateMediaItem(@PathVariable Long id, @RequestBody String body) {
        return "updated " + id;
    }
 
    // 'bob' có role VIEWER (ALLOW playback:start qua role) nhưng bị DENY
    // riêng qua user_permissions -> luôn nhận NOT_AUTHORIZED ở endpoint này.
    @RequirePermission("playback:start")
    @PostMapping("/{id}/play")
    public String startPlayback(@PathVariable Long id) {
        return "playback session created for " + id;
    }
}