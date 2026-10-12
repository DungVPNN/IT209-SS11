package com.rikkei.crmchat.dto;

/** DTO cho endpoint /api/v1/crm/chat. */
public final class ChatDtos {

    private ChatDtos() {}

    public record ChatRequest(String question) {}

    public record ChatResponse(String answer) {}

    /** Trả về khi có lỗi (VectorStore/LLM gián đoạn). */
    public record ErrorResponse(String error, String detail) {}
}
