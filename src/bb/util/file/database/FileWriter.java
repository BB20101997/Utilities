package bb.util.file.database;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * @author BB20101997
 */
@SuppressWarnings("javadoc")
public final class FileWriter implements ISaveAble {

	/**
	 * The Version of the FileWriter
	 */
	@SuppressWarnings("FieldCanBeLocal")
	public final float VERSION = 0.02f;

	// kann bis mindestens F8FF fortgeführt werden

	private static final char splitCharacter = '\uF8F0';
	private static final char typeSeparator  = '\uF8F5';

	private static final char innerFileWriterBeginn = '\uF8F7';
	public static final  char innerFileWriterEnd    = '\uF8F8';

	private static final char beginOfString = '\uF8F1';
	private static final char endOfString   = '\uF8F2';

	private static final char endOfFileWriter = '\uF8F4';

	//private static final char[] specialCharacters = {splitCharacter, typeSeparator, innerFileWriterBeginn, innerFileWriterEnd,
	//		beginOfString, endOfString, endOfFileWriter};

	public static final String VersionName = "\uF8F6" + "$VERSION$";

	private static final String LENGTH = "length";

	/**
	 * @author BB20101997
	 */
	public enum Types {
		INT, STRING, ISAVEABLE, BYTE, BOOLEAN, FLOAT, DOUBLE, SHORT, LONG, CHAR
	}

	private FileWriter() {

		add(VERSION, VersionName);
	}

	/**
	 * A list of Objects , to be saved or to be read
	 */
	private final List<Object> ObjectList  = new ArrayList<>();
	/**
	 * The names of the Objects for easy retrieval
	 */
	private final List<String> ObjectNames = new ArrayList<>();
	/**
	 * The Type of Object saved in the ObjectList
	 */
	private final List<Types>  ObjectType  = new ArrayList<>();

	private void toObject(String name, Types type, String s) {

		Object o;

		switch(type) {
			case INT: {
				o = Integer.valueOf(s);
				add((int) o, name);
				break;
			}
			case BYTE: {
				o = Byte.valueOf(s);
				add((byte) o, name);
				break;
			}
			case BOOLEAN: {
				o = Boolean.valueOf(s);
				add((boolean) o, name);
				break;
			}

			case FLOAT: {
				o = Float.valueOf(s);
				add((float) o, name);
				break;
			}
			case DOUBLE: {
				o = Double.valueOf(s);
				add((double) o, name);
				break;
			}
			case SHORT: {
				o = Short.valueOf(s);
				add((short) o, name);
				break;
			}
			case LONG: {
				o = Long.valueOf(s);
				add((long) o, name);
				break;
			}
			case CHAR: {
				o = s.charAt(0);
				add((char) o, name);
				break;
			}

		}


	}

	private void toObject(String name, Types type, FileWriter fw) {

		switch(type) {
			case ISAVEABLE: {
				add(fw, name);
				break;
			}
			case STRING: {
				Object o = fw.get(LENGTH);
				if(o instanceof Integer) {
					int length = (int) o;
					char[] carr = new char[length];
					char c;
					for(int i = 0; i < length; i++) {
						o = fw.get("" + i);
						if(o instanceof Character) {
							carr[i] = (char) o;
						}
						else{
							add((String)null,name);
							return;
						}
					}
					add(new String(carr), name);
				}
				break;
			}
		}

	}

	/**
	 * @param is the InputStream to read the Objects from
	 *
	 * @return if reading was successful
	 */
	boolean readFromStream(InputStream is) {
		InputStreamReader ISR = new InputStreamReader(is);

		char c;

		int i;

		int position = 0;// 0 if at the Name,1 if Type,2 if value

		String name = "";
		Types type = null;
		String s = "";

		boolean inString = false;

		FileWriter fw = null;
		try {
			loop:
			while((i = ISR.read()) != -1) {
				c = (char) i;

				if(!inString) {

					switch(c) {

						case endOfFileWriter:{
							break loop;
						}
						case innerFileWriterEnd: {
							continue loop;
						}

						case splitCharacter: {
							if(type == Types.ISAVEABLE || type == Types.STRING) {
								toObject(name, type, fw);

							} else {
								toObject(name, type, s);
							}
							position = 0;
							continue loop;
						}

						case innerFileWriterBeginn: {
							fw = new FileWriter();
							fw.readFromStream(is);
							continue loop;
						}
						case typeSeparator: {
							switch(position) {
								case 0: {
									name = s;
									break;
								}
								case 1: {
									type = Types.values()[Integer.valueOf(s)];
									break;
								}
							}
							s = "";
							position++;
							continue loop;
						}
					}
				}

				if(c == endOfString && s.length() != 0) {
					inString = false;
				}

				if(inString) {
					s += c;
				}

				if(c == beginOfString) {
					inString = true;
				}
			}
		} catch(
				IOException e
				)

		{
			e.printStackTrace();
		}

		return false;
	}

	/**
	 * @param os the OutputStream to be written to
	 *
	 * @return if the execution was successful
	 *
	 * @throws IOException is thrown when the writing of one Object to Steam fails
	 */
	void writeToStream(OutputStream os) throws IOException {

		for(int i = 0; i < ObjectList.size(); i++) {
			writeObjectToStream(ObjectList.get(i), ObjectType.get(i), ObjectNames.get(i), os);
		}
		os.write(endOfFileWriter);
	}

	/**
	 * @param obj  the object to be written
	 * @param t    the type of the Object
	 * @param name the name of the Object
	 * @param os   the OutputStream to be written to
	 *
	 * @return if the operation was successful
	 */
	private void writeObjectToStream(Object obj, Types t, String name, OutputStream os) throws IOException {


		os.write((name.replaceAll("" + typeSeparator, "") + typeSeparator + t.ordinal() + typeSeparator).getBytes());

		switch(t) {
			case ISAVEABLE: {
				os.write(innerFileWriterBeginn);
				ISaveAble is = (ISaveAble) obj;
				FileWriter fw = new FileWriter();
				is.writeToFileWriter(fw);
				fw.writeToStream(os);
				os.write(innerFileWriterEnd);
				break;
			}
			case STRING: {
				os.write(innerFileWriterBeginn);
				FileWriter fw = new FileWriter();
				int i = 0;
				fw.add(((String) obj).length(), LENGTH);
				for(char c : ((String) obj).toCharArray()) {
					fw.add(c, "" + i);
					i++;
				}
				fw.writeToStream(os);
				os.write(innerFileWriterEnd);
			}
			default: {
				os.write((beginOfString + String.valueOf(obj) + endOfString).getBytes());
				break;
			}
		}
		os.write(splitCharacter);

	}

	/**
	 * @param i    the Integer to be added
	 * @param name the Name of the Integer
	 */
	public void add(int i, String name) {

		ObjectList.add(i);
		ObjectNames.add(name);
		ObjectType.add(Types.INT);
	}

	/**
	 * @param i    the String to be added
	 * @param name the name of the String
	 */
	public void add(String i, String name) {

		ObjectList.add(i);
		ObjectNames.add(name);
		ObjectType.add(Types.STRING);
	}

	/**
	 * @param i    the ISaveAble to be added
	 * @param name the name of the ISaveAble
	 */
	public FileWriter add(ISaveAble i, String name) {

		FileWriter fw;
		ObjectList.add(fw = new FileWriter());
		ObjectNames.add(name);
		ObjectType.add(Types.ISAVEABLE);
		i.writeToFileWriter(fw);
		return fw;
	}

	/**
	 * @param i    the byte to be added
	 * @param name the name of the byte
	 */
	public void add(byte i, String name) {

		ObjectList.add(i);
		ObjectNames.add(name);
		ObjectType.add(Types.BYTE);
	}

	/**
	 * @param i    the boolean to be added
	 * @param name the name of the boolean
	 */
	public void add(boolean i, String name) {

		ObjectList.add(i);
		ObjectNames.add(name);
		ObjectType.add(Types.BOOLEAN);
	}

	/**
	 * @param i    the float to be added
	 * @param name the name of the float
	 */
	void add(float i, String name) {

		ObjectList.add(i);
		ObjectNames.add(name);
		ObjectType.add(Types.FLOAT);
	}

	/**
	 * @param i    the double to be added
	 * @param name the name of the double
	 */
	public void add(double i, String name) {

		ObjectList.add(i);
		ObjectNames.add(name);
		ObjectType.add(Types.DOUBLE);
	}

	/**
	 * @param i    the short to be added
	 * @param name the name of the short
	 */
	public void add(short i, String name) {

		ObjectList.add(i);
		ObjectNames.add(name);
		ObjectType.add(Types.SHORT);
	}

	/**
	 * @param i    the long to be added
	 * @param name the name of the long
	 */
	public void add(long i, String name) {

		ObjectList.add(i);
		ObjectNames.add(name);
		ObjectType.add(Types.LONG);
	}

	/**
	 * @param i    the chat to be added
	 * @param name the name of the char
	 */
	public void add(char i, String name) {

		ObjectList.add(i);
		ObjectNames.add(name);
		ObjectType.add(Types.CHAR);
	}

	public Object get(String name) {
		int index;
		if((index = ObjectNames.indexOf(name)) != -1) {
			return ObjectList.get(index);
		}
		return null;
	}

	public Types getObjectType(String name) {
		int index;
		if((index = ObjectNames.indexOf(name)) != -1) {
			return ObjectType.get(index);
		}
		return null;
	}

	@Override
	public void writeToFileWriter(FileWriter fw) {
		//Clearing Lists
		fw.ObjectList.clear();
		fw.ObjectNames.clear();
		fw.ObjectType.clear();
		//Filling Lists
		fw.ObjectList.addAll(ObjectList);
		fw.ObjectNames.addAll(ObjectNames);
		fw.ObjectType.addAll(ObjectType);
	}

	@Override
	public void loadFromFileWriter(FileWriter fw) {
		//Clearing Lists
		ObjectList.clear();
		ObjectNames.clear();
		ObjectType.clear();
		//Filling Lists
		ObjectList.addAll(fw.ObjectList);
		ObjectNames.addAll(fw.ObjectNames);
		ObjectType.addAll(fw.ObjectType);
	}
}