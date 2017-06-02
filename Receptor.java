/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package proyectoderedes.i;

import java.util.logging.Level;
import java.util.logging.Logger;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.TargetDataLine;
import javax.swing.JTextArea;


public class Receptor {
	
	//atributos:
	
	final int SAMPLING_RATE = 16*1024;/*Mismo formato que el emisor*/
	AudioFormat formato = new AudioFormat(SAMPLING_RATE, 8, 1, true, true);
	TargetDataLine targetDataLine;/*Para la tarjeta de Sonido*/
	public String TRAMAS="";/*Para concatenar todas las tramas en una sola*/
	public javax.swing.JTextArea Texto;/*Variable a la que se le añadira el texto desde la interfaz*/
	/*Para poder modificarla y añadirla al mensaje que se está recibiendo*/
	
	//métodos:
	 /*************************************************************************/
	  /***** ************CONSTRUCTOR DEL RECEPTOR **********************
	   *********** **************************************** ******************/
	  public Receptor(JTextArea chat) throws LineUnavailableException{
          
          Texto=chat;//Se le asigna al area de texto global e area de texto de la interfaz
          //Variable de linea de Audio con datos del formato
          DataLine.Info targetInfo = new DataLine.Info(TargetDataLine.class, formato);
          try{
        	  targetDataLine = (TargetDataLine) AudioSystem.getLine(targetInfo);
                  /*  EJECUTO EL HILO audio DE LA CLASE Audio() QUE SE ENCARGA DE 
                      RECIBIR Y PROCESAR LOS DATOS QUE LLEGAN POR LA LINEA */
                  Thread audio = new Audio();
                  audio.start();
          }catch(Exception e){ System.out.println(e);}
	  }
	  
	  /*************************************************************************/
	  /***** ************Cambia la secuencia completa en Binario a************
	   ********************* la Cadena del Mensaje final **********************
	   *********** **************************************** ******************/
	  public String aCadena(String cad){
    
          int n = cad.length()/7;
          int n2 []  = new int[n];
          int ini=0, fin=7;
          String mensaje="";

          for(int i=0; i<n; i++){
              String aux;
              aux = cad.substring(ini, fin);
              System.out.println(aux + " AUX");
              ini=fin;
              fin=fin+7;
              n2[i] = Integer.parseInt(aux, 2);
              mensaje = mensaje + Character.toString( (char) n2[i] );
          }
          
          return mensaje;
      }
	  
	  /*************************************************************************/
	  /***** ************Verifica que todo haya llegado sin errores**********
	   ********************************* en la Trama **********************
	   *********** **************************************** ******************/
	  public boolean RevisarCRC(String trama){
			String polinomio = "101";
			String TramaAux;
			int k;
			boolean band;
			
			while(trama.length() >= polinomio.length()){//Decodificación.
				k=0;
				TramaAux="";
				band=false;
				
			  while(k<=2){
				if((trama.charAt(k) < polinomio.charAt(k)) || (trama.charAt(k) > polinomio.charAt(k)) ){
						TramaAux+="1";
						band=true;
				}else if(band) TramaAux+="0";
				k++;
			  }
			  TramaAux+=trama.substring(3);
			  trama = TramaAux;
			}
			
			if(trama.isEmpty())	trama="0";
			if(trama.charAt(0)=='1') return false;	else return true;
	 }
		  
	  /*************************************************************************/
	  /***** ************HILO QUE SE ENCARGA DE RECIBIR Y **********************
	   *********** PROCESAR LOS DATOS QUE LLEGAN POR LA LINEA ******************/
	  /*************************************************************************/
	  class Audio extends Thread {
			//atributos:
			byte[] Buffer = new byte[targetDataLine.getBufferSize()];//Buffer encargado de recibir la linea
		    //métodos:        
			public void run(){
				String caracter = ""; //Concatena todos los bits hasta formar una trama (El caracter q recibio)
				boolean band = false;//Bandera que indica si conoce o no el bit que recibe
				int contador_positivos=0; //Cuenta la cant. de num. positivos (Para reconocer si es Bit 1 o Bit 0
		        boolean error=false; //Comprueba errores de transmisión
		        String auxiliar="";
		        try{
		        	targetDataLine.open(formato);//Abre la linea de Audio
		        }catch(LineUnavailableException ex){Logger.getLogger(Receptor.class.getName()).log(Level.SEVERE,null,ex);}
		        targetDataLine.start(); //Ejecuta la linea de Audio          
		        while(true){ //Mientras se reciba un mensaje el receptor se mantiene abierto...           
		            try{
		            	int i=0;
		            	while(i < 16){
		            		 targetDataLine.read(Buffer, 0, Buffer.length);
		            		 int j=0;
		            		 while(j < Buffer.length){
		            			 if(!band){
		            				 if(Buffer[j] > 85){ 
		            					/* System.out.println("BUFFER: "+Buffer[j]); */contador_positivos++;}
		            				 else if(Buffer[j] < 0 && contador_positivos > 3) band=true;
		            			 }
		            			j++; 
		            		 }
		            		 if(contador_positivos > 10 && contador_positivos < 21 && !error){
		            			 caracter+='1';
		            			 //System.out.println("Bit individual recibido: 1");
		                         i++;
		            		 }else if(contador_positivos > 0 && contador_positivos < 10 && !error){
		            			 caracter+='0';
		            			// System.out.println("Bit individual recibido: 0");
		                         i++;
		            		 }else if(error) error=false;
		            		 band=false;
		            		 contador_positivos=0;
		            	}
		            	if(!caracter.isEmpty()){
		            		System.out.println("TRAMA QUE LLEGA: " + caracter);
		            		 auxiliar=caracter.substring(1, 2);
		                     System.out.println("BANDERA: " + auxiliar);
		                     caracter = caracter.substring(7, 16);
		                     System.out.println("TRAMA DESPUES DE QUITAR LA BANDERA: " + caracter);
		                     if( RevisarCRC(caracter) && "1".equals(auxiliar) ){
		                         caracter = caracter.substring(0, 7);
		                         System.out.println("QUITANDO EL CRC: " + caracter);
		                         if(!caracter.matches("1111111")) TRAMAS+=caracter;
		                         else{
		                        	 System.out.println( aCadena(TRAMAS) ); 
		                             Texto.append("            << FromSender >>:  " + aCadena(TRAMAS) + "\n");
		                             TRAMAS="";
		                         } 
		            	     }else{
		            	    	 String aux=caracter.substring(1, 8);
		                         System.out.println("TRAMA INCORRECTA. "+aux);
		                         error=true; 
		                         if( !aux.matches("1111111") ) TRAMAS+= aux;
		                         else{
		                        	 System.out.println( aCadena(TRAMAS) ); 
		                             Texto.append("            << FromSender >>:  " + aCadena(TRAMAS) + "\n");
		                             TRAMAS=""; 
		                         }
		            	     }
		                     caracter="";
		                     auxiliar="";
		                  } 
		            }catch(Exception e){System.out.println(e);}
		            targetDataLine.close();// EN CASO DE EMERGENCIA QUITAR COMENTARIO
		         }
			} 
		}

	
}



