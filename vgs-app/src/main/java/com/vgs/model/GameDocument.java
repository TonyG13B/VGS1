<content><![CDATA[
package com.vgs.model;

import lombok.Data;
import java.util.List;
import java.util.Map;

@Data
public class GameDocument {
    private String id;
    private String name;
    private Map<String, Object> details; // Embedded details
    private List<Map<String, Object>> transactions; // Embedded transactions (limited size)
}
]]></content>
