import java.util.ArrayList;


public class SemanticRecord {
	
	public String idName; // lexeme
	public String idType; // int, float, class type
	public String idKind; // variable, parameter, function, class, numliteral
	
	public int memorySize = 1; //scales from array size then data type
	public String memoryAddress; //unique memory label
	
	//variable/parameter
	public ArrayList<Integer> dimensions = new ArrayList<Integer>();
	
	//function
	//pointer to parameters in function local table
	public ArrayList<SemanticRecord> parameterReferences = new ArrayList<SemanticRecord>(); 
	
	//function/class
	public SymbolTable symbolTableReference;
	
	//second pass numliteral info
	public int intValue;
	public float floatValue;
	
	//second pass array info
	int offset;
	int indiceIndex;
	int dimensionCounter;
	
	public SemanticRecord() {}

	public SemanticRecord(String idKind) {
		this.idKind = idKind;
	}
	
	//used for creating integer literals second pass
	public SemanticRecord(String idKind, String idType, int intValue) {
		this.idKind = idKind;
		this.idType = idType;
		this.intValue = intValue;
	}
	
	public void updateOffset(int indiceValue) {
		int product = 1; //product of N(k+1) -> N(d)
		for(int index = indiceIndex + 1; index < dimensions.size(); index++) {
			product *= dimensions.get(index);
		}
		offset += indiceValue*product;
		indiceIndex++;
	}
	
	
	//used for creating float literals second pass
	public SemanticRecord(String idKind, String idType, float value) {
		this.idKind = idKind;
		this.idType = idType;
		this.floatValue = floatValue;
	}
	
	//used for synthesized attribute migration
	public void copyAttributes(SemanticRecord semRec) {
		idName = semRec.idName;
		idType = semRec.idType;
		idKind = semRec.idKind;
		memoryAddress = semRec.memoryAddress;
		memorySize = semRec.memorySize;
		intValue = semRec.intValue;
		floatValue = semRec.floatValue;
		indiceIndex = semRec.indiceIndex;
		dimensions = semRec.dimensions;
		parameterReferences = semRec.parameterReferences;
	}
	
	@Override
	public boolean equals(Object other) {
		if(!(other instanceof SemanticRecord)) {
			return false;
		}
		SemanticRecord semanticRecord = (SemanticRecord) other;		
		return this.idName.equals(semanticRecord.idName);
	}
	
	public SemanticRecord(String idName,String idKind, String idType, String memoryAddress) {
		this.idName =idName;
		this.idKind = idKind;
		this.idType = idType;
		this.memoryAddress = memoryAddress;
	}
	
	//array cannot have zero size
	public void scaleMemorySize(int factor) {
		memorySize *= factor;
	}
	
	public void incDimensionCounter() {
		dimensionCounter++;
	}
	
	public void addParameterReference(SemanticRecord parSemRec) {
		parameterReferences.add(parSemRec);
	}
	
	//getter and setters
	public String getIdName() {
		return idName;
	}

	public void setIdName(String idName) {
		this.idName = idName;
	}

	public String getIdType() {
		return idType;
	}

	public void setIdType(String idType) {
		this.idType = idType;
	}

	public String getIdKind() {
		return idKind;
	}

	public void setIdKind(String idKind) {
		this.idKind = idKind;
	}

	public int getMemorySize() {
		return memorySize;
	}

	public void setMemorySize(int memorySize) {
		this.memorySize = memorySize;
	}

	public String getMemoryAddress() {
		return memoryAddress;
	}

	public void setMemoryAddress(String memoryAddress) {
		this.memoryAddress = memoryAddress;
	}

	public SymbolTable getSymbolTableReference() {
		return symbolTableReference;
	}

	public void setSymbolTableReference(SymbolTable symbolTableReference) {
		this.symbolTableReference = symbolTableReference;
	}
	
	//testing purposes
	public void printSemRecInfo() {
		System.out.println("name: " + idName);
		System.out.println("kind: " + idKind);
		System.out.println("type: " + idType);
		System.out.println("num params: " + parameterReferences.size());
		System.out.println("dimension: " + dimensions.size());
		System.out.println("dimension counter: " + dimensionCounter);
		System.out.println();
	}

}
