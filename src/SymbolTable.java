import java.util.LinkedHashMap;


public class SymbolTable {
	public String name;
	public SymbolTable parentSymbolTable;
	public LinkedHashMap<String, SemanticRecord> semanticRecordEntries = new LinkedHashMap<String, SemanticRecord>();
	
	public void setName(String name) {
		this.name = name;
	}
}
