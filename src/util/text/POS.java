package util.text;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import util.datastructure.Tuple;

public class POS {
	public static String [][] names = { {"nc", "자립명사"}, {"np", "대명사"}, {"nb", "의존명사"}, {"nn", "수사"}, {"pv", "동사"},
			null, {"pa", "형용사"}, null, {"mag", "일반부사"}, null, 
			null, {"ii", "감탄사"}, {"jc", "격조사"}, {"co", "지정사"}, {"ef", "종결어미"},
			{"ep", "선어말어미"}, {"xp", "접두사"}, {"xsn", "명사파생접미사"}, {"xsv", "동사파생접미사"}, {"xsm", "형용사파생접미사"},
			{"xsn", "명사파생접미사"}, {"s", "기호"}, {"px", "보조용언"}, {"mm", "관형사"}, {"ec", "연결어미"},
			{"jj", "접속조사"}, {"jm", "속격조사"}, {"maj", "접속부사"}, {"jx", "보조사"}, {"etm", "관형형어미"},
			{"etn", "명사형어미"}, {"uk", "미등록어"}, {"nk", "사용자사전등록명사"}, {"nr", "고유명사"} 
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
