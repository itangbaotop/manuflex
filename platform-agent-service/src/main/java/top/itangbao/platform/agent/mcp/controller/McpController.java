package top.itangbao.platform.agent.mcp.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import top.itangbao.platform.agent.mcp.protocol.McpRequest;
import top.itangbao.platform.agent.mcp.protocol.McpResponse;
import top.itangbao.platform.agent.mcp.server.McpServer;

@RestController
@RequestMapping("/api/mcp")
@RequiredArgsConstructor
public class McpController {
    
    private final McpServer mcpServer;
    
    @PostMapping("/request")
    public McpResponse handleRequest(@RequestBody McpRequest request) {
        return mcpServer.handleRequest(request);
    }
    
    @GetMapping("/tools")
    public McpResponse getTools() {
        McpRequest request = McpRequest.builder()
            .method("tools/list")
            .build();
        return mcpServer.handleRequest(request);
    }
}