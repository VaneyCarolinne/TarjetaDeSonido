/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package proyectoderedes.i;

/*************************************/
/***Uso de la Librería JAVA SOUND*****/
/*************************************/
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.SourceDataLine;
/********************************************************************************/
/***Uso de la Librereía FILE para guardar las tramas en binario en un archivo****/
/********************************************************************************/
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;


/*************************************/
/***********Clase Emisor*************/
/*************************************/
public class Emisor{
	//atributos:
	protected static final int SAMPLE_RATE = 16*1024;
	//métodos:
	/***************************/
	/**Constructor del Emisor**/
	/***************************/
        
    /**
     *
     * @throws javax.sound.sampled.LineUnavailableException
     */
    public Emisor(String mensaje) throws LineUnavailableException, IOException{
		String mensajeFinal="";
		File archivo=new File("TRAMAS.txt");
                FileWriter escribir=new FileWriter(archivo,true);
                for(int i=0;i<mensaje.length();i++){
                         escribir.write(TramaConvert (mensaje.charAt(i)));
                         escribir.write("\n");
                         this.Enviar( TramaConvert (mensaje.charAt(i)) );
		}	
		
               mensajeFinal=CRC("1111111");
               escribir.write("0111110"+mensajeFinal);
               escribir.write("\n");
               escribir.close();
               this.Enviar("0111110"+mensajeFinal);
	}
	/*************************************/
	/**Convierte un caracter a una trama**/ //Formato Trama-Relleno-CRC-BAND
	/*************************************/
	public static String TramaConvert(char cad){
		int aux=(int)cad; // Transforma el char a un entero (ASCII)
		
		//De entero a Binario
		String Cadena_Binario = Integer.toBinaryString(aux); 
                
		
		if(Cadena_Binario.length()<7){//Validación de que haya secuencias de caracteres de 7 bits
			while(Cadena_Binario.length()<7){
				Cadena_Binario = "0"+Cadena_Binario;//rellenando con 0's al final. 
			}										
		}
		//Se aplica la suma de verificación CRC a la cadena
		Cadena_Binario = CRC(Cadena_Binario); 
		//Se le asigna la bandera a la cadena
		Cadena_Binario =  "0111110"+Cadena_Binario;      
                
		return Cadena_Binario;
	}
	/*************************************/
	/**Detecta los errores en la trama**/
	/*************************************/
    public static String CRC(String trama){
    	String polinomio = "101"; //M(x) = x^2 + 1 Polinomio generador
    	String tramaEmisora = trama + "00"; // Agrega una cantidad de 0's según el grado de M(x) al final de la trama 
    	String tramaAux; // Trama Auxiliar
    	int k; // indice que termina según el grado del polinomio
    	boolean band; //verifica el final de la trama
	
    	while(tramaEmisora.length() >= polinomio.length()){
    		k=0;
    		tramaAux="";
    		band=false;
    		while(k<=2){//Que recorra toda la trama según el grado del polinomio
    			// SI LOS DIGITOS SON IGUALES ES UN 0 SI SON DIFERENTES ES UN 1
    			if( (tramaEmisora.charAt(k) < polinomio.charAt(k)) || (tramaEmisora.charAt(k) > polinomio.charAt(k)) ){
    				tramaAux+="1";
    				band=true;
    			}else{
    				if(band) tramaAux+="0";/* La bandera se utiliza para que no se acumulen 
					0 al principio de la trama sino despuÃ©s del primer 1 */
    			}
    			k++;
    		}
    		// MEZCLAMOS EL RECIDUO CON LA TRAMA ANTERIOR
    		tramaAux+=tramaEmisora.substring(3); 
    		 // ASIGNAMOS LA NUEVA TRAMA
    		tramaEmisora=tramaAux;
    	}	
    	// SI EL RESIDUO ES MENOR QUE 4 SE RELLENA DE 0 PARA QUE ENCAJE EN LA TRAMA	
	   while(tramaEmisora.length() <= 1) tramaEmisora = "0"+tramaEmisora;
	   tramaEmisora = trama + tramaEmisora; // trama que se va a Enviar
	   
	   return tramaEmisora;
    }
    
    /******************************************/
    /**Envia la trama a travéz de una Linea:**/
    /*****************************************/
    public void Enviar(String trama) throws LineUnavailableException{
    	//Formato de Audio lineal por modulación por impulsos codificados ...
    	final AudioFormat frecuencia_Audio = new AudioFormat(SAMPLE_RATE, 8, 1, true, true);
    	//Parametros del formato:
    	//(frecuencia de muestreo, cantidad de bits por muestra, cantidad de canales, signed o unsigned, big-endian o no)
    	
    	SourceDataLine linea = AudioSystem.getSourceDataLine(frecuencia_Audio);
    	linea.open(frecuencia_Audio,SAMPLE_RATE);//Abro la linea de Audio
    	linea.start();//Ejecuto la linea de Audio
    	                          //**************************************/
                                  //***Ciclo de transmisión de la trama:***/
                                 //****************************************/
        System.out.println("TRAMA EN TRANSMISIÓN: "+trama); 
        for(int i=0; i<trama.length();i++){
    		//System.out.println("BIT EN TRANSMISIÓN: " + trama.charAt(i));
    		int frecuencia;
    		//Si es 1 modula con frecuencia 400 sino si es 0 modula con 1000
    		if(trama.charAt(i)=='1') frecuencia=400; else frecuencia=1000;
    		
    		byte[] Buffer = createSinWaveBuffer(frecuencia);//Generando Onda Senoidal y guarda sus valores en el buffer
    		 int count = linea.write(Buffer, 0, Buffer.length); // ENVIO el Buffer POR LA LINEA
    		//for (int j = 0; j < Buffer.length; j++) System.out.println(Buffer[j]); // Ver Onda por pantalla
    	}	
        linea.drain();
    }
    
    /***************************************************************************/
    /******************* FUNCION QUE CREA ONDA SENOIDAL ************************/
    /******************    Y LA ALMACENA EN UN BUFFER   ************************/
    /***************************************************************************/
     public static byte[] createSinWaveBuffer(double freq) { 
         int samples = (int) ((50 * SAMPLE_RATE) / 100);
         byte[] output = new byte[samples];
         
         double period = (double) SAMPLE_RATE / freq;
         double period2=0.0;
         for (int i = 0; i < output.length; i++) {
            /*
             OJO: LA PRIMERA QUINTA PARTE DEL BUFFER ALMACENA LOS VALORES DE LA 
             ONDA CON LA FRECUANCIA RECIBIDA freq Y EL PERIODO CALCULADO period 
             EL RESTO DEL BUFFER ALMACENA VALORES 0 DEL PERIODO period2=0.0
             */
            if(i<(output.length/5)){
                double angle = 2.0 * Math.PI * i / period;
                output[i] = (byte) (Math.sin(angle) * 127f);
            }else{
                double angle = 2.0 * Math.PI * i / period2;
                output[i] = (byte) (Math.sin(angle) * 127f);
            }
         }
         return output;
     }
  
     
    
}



