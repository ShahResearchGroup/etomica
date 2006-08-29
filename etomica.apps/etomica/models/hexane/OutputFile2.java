package etomica.models.hexane;

import java.io.*;

public class OutputFile2 {

    String fileName;
    PrintWriter pw;

    public OutputFile2 (String name) {
	fileName = new String(name);
	try {
	    pw = new PrintWriter(new FileWriter(fileName));
	} catch (IOException ioe) {
	    System.err.println("Cannot open file: " + fileName);
	}
    }

    public void print (String str) {
	if (pw != null)
	    pw.print(str);
    }

    public void println (String str) {
	if (pw != null)
	    pw.println(str);
    }

    public void println (int i) {
	if (pw != null)
	    pw.println(i);
    }

    public void close () {
	if (pw != null)
	    pw.close();
    }

    public static void main (String[] args) {
	OutputFile2 of = new OutputFile2("test.data");
	of.println(10 + "this" + " is only a test");
	of.println(10);
	of.close();
    }

}

