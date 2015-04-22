import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Scanner;


public class Driver {
	
	public static void main(String[] args) {
		
		String input = null;
		String fileName; 
		
		fileName = readFileName();
		
		
		try {
			
			//read program input from file
			input = readFile(fileName, Charset.defaultCharset());
			
			System.out.println("COMPILING FILE : " + fileName);
			System.out.println();
			
			Compiler.run(input);
			
			//output to file paths : token_sequence, derivation_sequence, symbol_table, moon_output
			
			writeToFile("OutputFiles/token_sequence", LexicalAnalyzer.tokenSequence);
			writeToFile("OutputFiles/derivation_sequence", FirstPassParser.derivationSequence);
			writeSymbolTableToFile("OutputFiles/symbol_table", SemanticAnalyzer.symbolTableList);
			writeToFile("OutputFiles/moon_output", CodeGenerator.moonInstructionList);
			
		} catch (IOException e) {
			System.out.println("Eror File Not Found");
		}
	}
	
	public static String readFileName() {
		Scanner kb = new Scanner(System.in);
		String filename = "InputFiles/";
		System.out.print("ENTER FILENAME: ");
		filename = filename + kb.nextLine();
		kb.close();
		return filename;
	}
	
	public static String readFile(String path, Charset encoding) throws IOException {
		byte[] encoded = Files.readAllBytes(Paths.get(path));
		return new String(encoded, encoding);
	}
	
	public static void writeToFile(String path, ArrayList<String> output) {
		PrintWriter derivationWriter;
		try {
			derivationWriter = new PrintWriter(path, "UTF-8");
			for(String line : output) {
				derivationWriter.println(line);
			}	
			derivationWriter.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}	
	}
	
	public static void writeSymbolTableToFile(String path, ArrayList<SymbolTable> output) {
		
		try {
			PrintWriter entryWriter = new PrintWriter(path, "UTF-8");
			for(SymbolTable symbolTable : output) {
				entryWriter.println("Symbol table: " + symbolTable.name);
				entryWriter.println("#Entries: " + symbolTable.semanticRecordEntries.size());
				//symbol table fields
				entryWriter.format("%-20s%-20s%-20s","name","kind","type");
				entryWriter.format("%-25s%-20s%-20s","symbol table reference","dimension","num params");
				entryWriter.format("%-25s%-20s%-20s","param types","memory address","memory size");
				entryWriter.println();
				for(SemanticRecord semanticRecord : symbolTable.semanticRecordEntries.values()) {
					entryWriter.format("%-20s%-20s%-20s",semanticRecord.idName,semanticRecord.idKind,semanticRecord.idType);
					entryWriter.format("%-25s%-20s%-20s",semanticRecord.symbolTableReference,semanticRecord.dimensions.size(),semanticRecord.parameterReferences.size());
					
					//print parameter types for functions
					StringBuilder paramTypes = new StringBuilder();
					for(SemanticRecord semRec: semanticRecord.parameterReferences) {
						paramTypes.append((semRec.getIdType() + " "));
					}
					
					entryWriter.format("%-25s%-20s%-20s",paramTypes,semanticRecord.memoryAddress,semanticRecord.memorySize);
					
					entryWriter.println();
				}
				entryWriter.println();
			}		
			entryWriter.close();
		} catch (FileNotFoundException | UnsupportedEncodingException e) {
			e.printStackTrace();
		}
	}
}
