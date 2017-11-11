package br.com.relatorio.sonar.utils;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * 
 * @author MARCOS
 *
 */
public class Util {

	public static String formatarData(Date date) {
		String dataFormatada = "";
		if (date != null) {
			dataFormatada = new SimpleDateFormat("dd/MM/yyyy").format(date);
		}
		return dataFormatada;
	}

}
