package util.text;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import util.datastructure.Tuple;

public class POS {
	public static String [][] names = { {"nc", "�ڸ����"}, {"np", "����"}, {"nb", "�������"}, {"nn", "����"}, {"pv", "����"},
			null, {"pa", "�����"}, null, {"mag", "�Ϲݺλ�"}, null, 
			null, {"ii", "��ź��"}, {"jc", "������"}, {"co", "������"}, {"ef", "������"},
			{"ep", "������"}, {"xp", "���λ�"}, {"xsn", "����Ļ����̻�"}, {"xsv", "�����Ļ����̻�"}, {"xsm", "������Ļ����̻�"},
			{"xsn", "����Ļ����̻�"}, {"s", "��ȣ"}, {"px", "�������"}, {"mm", "������"}, {"ec", "������"},
			{"jj", "��������"}, {"jm", "�Ӱ�����"}, {"maj", "���Ӻλ�"}, {"jx", "������"}, {"etm", "���������"},
			{"etn", "��������"}, {"uk", "�̵�Ͼ�"}, {"nk", "����ڻ�����ϸ��"}, {"nr", "�������"} 
			};
	
	public enum Type { Number, English, Korean };
	
	public static String name(int id) {
		if (names[id] != null) return names[id][1];
		else return null;
	}
	
	public static String engName(int id) {
		if (names[id] != null) return names[id][0];
		else return null;
	}
	
	public static String replaceMorpText(String text, List<Tuple<String,String>> patterns) {
		for (Tuple<String,String> pattern : patterns) {
			text = text.replaceAll(pattern.first(), pattern.second());
		}
		return text;
	}
	
	public static String changePOSType(String text, Type fromType, Type toType) {
		String newText = "";
		Matcher m = Pattern.compile("([\\S]+)/([^/]+) ").matcher(text);
		if (fromType == Type.Number) {
			if (toType == Type.English)
				while (m.find()) { newText += m.group(1) + "/" + POS.engName(Integer.valueOf(m.group(2))) + " "; }
			else if (toType == Type.Korean)
				while (m.find()) { newText += m.group(1) + "/" + POS.name(Integer.valueOf(m.group(2))) + " "; }
		}
		else {
			// Not implemented
		}
		return newText;
	}
	
	public static void main(String [] args) throws Exception {
	}
}
