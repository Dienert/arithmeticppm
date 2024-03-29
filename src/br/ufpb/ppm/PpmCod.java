package br.ufpb.ppm;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Vector;

import com.colloquial.arithcode.ArithEncoder;
import com.colloquial.arithcode.BitOutput;

public class PpmCod {

	private static int maiorContexto = 3; //contexto default
	private static int tamanhoDoGrupoDeBits = 8; //tamanho default
	private static String arquivoDeSaida;

	//posicao dos argumentos do programa
	private static final int POSICAO_ARGUMENTO_MAIOR_CONTEXTO = 1;
	private static final int POSICAO_ARGUMENTO_TAMANHO_DO_GRUPO_DE_BITS = 2;
	private static final int POSICAO_ARGUMENTO_SAIDA = 3;

	private static Trie arvores[];
	private static String palavraAtual[];
	private static ArithEncoder codificador[];
	private static FileOutputStream fos[];
	private static FileOutputStream fosFinal;
	private static Vector<Vector<Character>> valoresCodificados;
	private static int totalContextoMenosUm[];

	static int parada = 0; // para testes


	/**
	 * 
	 * O programa deve ser chamado passando-se parâmetros.<br/>
	 * Uso: PpmCod <i>arquivo</i> <i>[maior contexto]</i> <i>[tamanho do grupo de bits]</i> <i>[nome do arquivo de saída (sem extensão)]</i>
	 * 
	 * @param args  
	 * 
	 */
	public static void main(String[] args) {

		if (args.length < 1 || args.length > 4) {
			System.out.println("Uso: PpmCod arquivo maior_contexto tamanho do grupo de bits [nome do arquivo de saída (sem extensão)]");
			System.out.println(" ou: PpmCod arquivo maior_contexto [tamanho do grupo de bits]");
			System.out.println(" ou: PpmCod arquivo [maior contexto]");
			System.out.println(" ou: PpmCod arquivo");
			System.exit(0);
		}

		File file = new File(args[0]);
		FileInputStream fis = null;
		//FileOutputStream testeEscrita = null;
		try {
			fis = new FileInputStream(file.getCanonicalPath());
			//testeEscrita = new FileOutputStream("testeEscrita.txt");
		} catch (FileNotFoundException e) {
			System.err.println("Arquivo não encontrado");
			System.exit(0);
		} catch (IOException e) {
			e.printStackTrace();
		}

		if (args.length >= POSICAO_ARGUMENTO_MAIOR_CONTEXTO+1) {
			int aux = Integer.parseInt(args[POSICAO_ARGUMENTO_MAIOR_CONTEXTO]);
			if (aux >= 0 && aux < 128) {
				maiorContexto = aux;
			} else {
				System.err.print("Contexto inválido. Utilizando contexto padrão [");
				System.err.println(maiorContexto + "]");
			}
		}

		if (args.length >= POSICAO_ARGUMENTO_TAMANHO_DO_GRUPO_DE_BITS+1) {
			int aux = Integer.parseInt(args[POSICAO_ARGUMENTO_TAMANHO_DO_GRUPO_DE_BITS]);
			if (aux == 1 || aux == 2 || aux == 4 || aux == 8 || aux == 16)
				tamanhoDoGrupoDeBits = aux;
			else {
				System.err.println("Número inválido para o tamanho do grupo de bits");
				System.err.println("Números válidos: 1, 2, 4, 8 ou 16");
				System.exit(0);
			}
		}

		if (args.length == POSICAO_ARGUMENTO_SAIDA+1) {
			arquivoDeSaida = args[POSICAO_ARGUMENTO_SAIDA];
		} else {
			int indiceAux = args[0].lastIndexOf('.');
			arquivoDeSaida = (indiceAux != -1 ) ? 
					args[0].substring(0, indiceAux) + "cod" :
						args[0] + "cod";
		}

		int numeroDeGruposDeBits = (tamanhoDoGrupoDeBits) < 8 ? 8/tamanhoDoGrupoDeBits : 1;

		arvores = new Trie[numeroDeGruposDeBits];
		palavraAtual = new String[numeroDeGruposDeBits];
		codificador = new ArithEncoder[numeroDeGruposDeBits];
		fos = new FileOutputStream[numeroDeGruposDeBits];
		valoresCodificados = new Vector<Vector<Character>> ();
		totalContextoMenosUm = new int[numeroDeGruposDeBits];
		File temporario = null;
		long tempoAntes, tempoDepois;
		
		// inicialização das variáveis
		for (int i = 0; i < numeroDeGruposDeBits; i++) {
			arvores[i] = new Trie(tamanhoDoGrupoDeBits);
			palavraAtual[i] = "";
			totalContextoMenosUm[i] = (int) Math.pow (2, tamanhoDoGrupoDeBits);

			if (numeroDeGruposDeBits > 1) {
				valoresCodificados.add(new Vector<Character> (totalContextoMenosUm[i]));
			} else if (tamanhoDoGrupoDeBits == 8){
				valoresCodificados.add(new Vector<Character> (128, 128));
			} else {
				valoresCodificados.add(new Vector<Character> (512, 512));
			}

			//System.out.println("Nome: " + nome);
			String nome = (numeroDeGruposDeBits > 1) ?
					arquivoDeSaida + i :
						arquivoDeSaida;
			try {
				if (i == 0) { // o primeiro arquivo é temporário (para que o cabeçalho possa ser inserido depois)
					temporario = File.createTempFile(nome, "tmp");
					temporario.deleteOnExit();
					fos[i] = new FileOutputStream(temporario);
					fosFinal = new FileOutputStream(nome + ".txt");
				} else
					fos[i] = new FileOutputStream (nome + ".txt");
			} catch (FileNotFoundException e) {
				e.printStackTrace();
				System.err.println("Problema na criação dos arquivos de saída.");
				System.exit(0);
			} catch (IOException e) {
				e.printStackTrace();
				System.err.println("Problema na criação dos arquivos de saída.");
				System.exit(0);
			}

			codificador[i] = new ArithEncoder(new BitOutput(fos[i]));
		}

		byte[] dataBlock = new byte[1024];
		byte [] bits;
		char chAux;
		int bytesLidos = 0;

		tempoAntes = System.currentTimeMillis();
		System.out.println("Iniciando codificação...");
		try {
			int numBytes;

			while((numBytes = fis.read(dataBlock)) != -1) {
				for (int i=0; i < numBytes; i++) {
					//String[] bits = splitCode(getCode(dataBlock[i]), tamanhoDoGrupoDeBits);
					bits = splitCode(dataBlock[i], tamanhoDoGrupoDeBits);
					bytesLidos++;
					for (int j = 0; j < bits.length; j++) {
						//System.out.println("Valor atual: " + bits[j]);

						// se o valor do byte for negativo, faz com que fique com o valor positivo,
						// necessario pois as posicoes do array em Trie sao todas positivas
						chAux = (char) ((bits[j]) >= 0 ? bits[j] : 256 + bits[j]);

						//System.out.println("Char: " + (int) chAux);

						if ((tamanhoDoGrupoDeBits == 16) && (++i < numBytes)) {
							int aux = (dataBlock[i] >= 0) ? (dataBlock[i] << 8) | chAux
									: ((256 + dataBlock[i]) << 8) | chAux; 
							chAux = (char) aux;
							bytesLidos++;
						}
						
						// atualiza palavra
						if (palavraAtual[j].length() <= maiorContexto) {
							palavraAtual[j] += chAux;
						} else {
							palavraAtual[j] = palavraAtual[j].substring(1) + chAux;
						}

						if (palavraAtual[j].length() == 0) continue;

						comprime (codificador[j], palavraAtual[j], arvores[j], j, new StringBuffer());

						//if (++parada == 100) {
						//arvores[j].percorre();
						//System.exit(0);
						//}

					}
				}
				//for (int i = 0; i < numeroDeGruposDeBits; i++) {
				//arvores[i].percorrePorNivel();
				//arvores[i].percorre();
				//}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		tempoDepois = System.currentTimeMillis();
		
		// fecha os arquivos
		try {
			fis.close();
			for (int i = 0; i < numeroDeGruposDeBits; i++) {
				codificador[i].close();
				fos[i].close();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		// escreve no arquivo final
		escreveArquivo (fosFinal, fos[0], temporario, bytesLidos);
		System.out.println("Codificação concluída em: " + (tempoDepois - tempoAntes) / 1000.0 + "s");
	}

	public static void comprime(ArithEncoder aritmetico, String s, Trie arvore, int j, StringBuffer passagem) {
		StringBuffer exclusao = (passagem != null) ? passagem : new StringBuffer();
		PseudoNo paiAux = new PseudoNo(); // para podermos receber o pai por referencia
		//Node paiAux = null;
		int[] lht;

		if (s.length() == 0) return;
		
		//System.out.println("Comprimindo: " +s);
		lht = arvore.getLowHighTotal(s, true, paiAux, exclusao);
		//System.out.println("Exclusao: " + exclusao);
		Node pai = paiAux.no;

		if (lht[0] == 0 && lht[1] == 0 && lht[2] == 0) { // não há nada no contexto, inserir e pular para o próximo
			arvore.insereEmNo(pai, s.charAt(s.length()-1));
			if (s.length() == 1) {
				//System.out.println("Codificando em menos um: " +s);
				comprimeContextoMenosUm (aritmetico, s, j);
				return;
			}
			comprime(aritmetico, s.substring(1), arvore, j, exclusao);
			return;
		}
		else if ((pai.totalEscape > 0 && lht[1] == lht[2])) { // deve ser codificado um escape
			//System.out.println("Codificando escape em " + s);
			try {
				//System.out.println("Codificando 1: ");
				//TestTrie.mostra(lht);
				aritmetico.encode(lht);
			} catch (IOException e) {
				e.printStackTrace();
			}

			arvore.insereEmNo(pai, s.charAt(s.length()-1)); // se veio um escape
			// é pq o símbolo não existe no contexto atual
			if (s.length() == 1) {
				//System.out.println("Apos codificar escape, codificando em menos um: " + s);
				comprimeContextoMenosUm (aritmetico, s, j);
				return;
			}
			comprime (aritmetico, s.substring(1), arvore, j, exclusao);
			return;
		}
		else { // codificar o simbolo de fato
			//System.out.println("Codificando " +s);
			try {
				//System.out.println("Codificando 2: ");
				//TestTrie.mostra(lht);
				aritmetico.encode(lht);
			} catch (IOException e) {
				e.printStackTrace();
				System.err.println("Problema na codificação.");
				System.exit(0);
			}

			for(int k = 0; k < s.length()-1; k++) {
				if (!arvore.procura(s.substring(k+1))) // se for encontrado, já aumenta o contador
					arvore.insere(s.substring(k+1)); // insere com contador 1 no contexto atual
			}
		}
	}

	// comprimindo para o contexto menos um	
	public static void comprimeContextoMenosUm (ArithEncoder aritmetico, String s, int j) {
		if (s.length() > 1) return;

		// pega a lista de valores que já foram codificados
		Vector<Character> valores = valoresCodificados.get(j);
		char ch = s.charAt(0);

		//System.out.print("Comprimindo " + (int) ch + "|");

		int lht[] = new int[3];
		lht[0] = (int) ch;
		lht[1] = (int) ch + 1;
		lht[2] = totalContextoMenosUm[j]--;

		for (int i = 0; i < valores.size(); i++) {
			//System.out.print(" " + (int) valores.get(i).charValue());
			if (valores.get(i).charValue() <= ch) {
				lht[0]--;
				lht[1]--;
			}
		}

		//System.out.println(" valor final: " + lht[0]);

		valores.add(ch);

		//System.out.println("Valores: ");
		//System.out.println(lht[0] + " " + lht[1] + " " +lht[2]);

		try {
			//System.out.println("Codificando 3: ");
			//TestTrie.mostra(lht);
			aritmetico.encode(lht);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static void escreveArquivo (FileOutputStream fosFinal, FileOutputStream fosTemp, File temp, int bytesLidos) {
		byte aux;
		int aux2;

		//System.out.println(bytesLidos);

		byte dataBlock[] = new byte[1024];
		int numeroLidos;
		try {
			aux2 = bytesLidos >> 24; // byte mais significativo
			fosFinal.write(aux2);
			//System.out.println(aux2);
			aux2 = bytesLidos >> 16 & 0xFF;
			fosFinal.write(aux2);
			//System.out.println(aux2);
			aux2 = bytesLidos >> 8 & 0xFF;
			fosFinal.write(aux2);
			//System.out.println(aux2);
			aux2 = bytesLidos & 0xFF;
			fosFinal.write(aux2);
			//System.out.println(aux2);
			aux = (byte) maiorContexto;
			fosFinal.write(aux);
			aux = (byte) tamanhoDoGrupoDeBits;
			fosFinal.write(aux);

			FileInputStream lerTemp = new FileInputStream(temp);

			while ((numeroLidos = lerTemp.read(dataBlock)) != -1) {
				for (int i = 0; i < numeroLidos; i++) {
					fosFinal.write(dataBlock[i]);
				}
			}

			fosTemp.close();
			fosFinal.close();
			lerTemp.close();
		} catch (IOException e) {
			e.printStackTrace();
			System.err.println("Problema na escrita.");
			System.exit(0);
		}
	}

	/**
	 * 
	 * Divide o código em grupos iguais do tamanho do argumento <i>tamanhoDoGrupo</i>
	 * @param code Código a ser dividido
	 * @param tamanhoDoGrupo Tamanho dos grupos de bits resultantes da divisão
	 * @return Um array de inteiros contendo os bits divididos.<br />
	 * Os bits mais significativos ficam nas posições menores<br />
	 * Os bits menos significativos ficam nas posições maiores. 
	 * 
	 */
	public static byte[] splitCode(byte code, int tamanhoDoGrupo) {
		int numeroDeGrupos = (tamanhoDoGrupo < 8) ? 8/tamanhoDoGrupo : 1;
		if (numeroDeGrupos == 1) return new byte[] {code};

		byte [] result = new byte[numeroDeGrupos];
		// anula para 8 grupos: 0x01, para 4: 0x03, para 2: 0x0F
		byte anula = (byte) ((tamanhoDoGrupo == 1) ? 1 : tamanhoDoGrupo * tamanhoDoGrupo - 1);

		//System.out.println("Lido: " + code);

		for (int i = 0, j = numeroDeGrupos-1; i < numeroDeGrupos; i++, j--) {
			result[j] = (byte) ((code >> (i * tamanhoDoGrupo)) & anula);
			//System.out.println("Dividido: " + result[j]);
		}
		return result;
	}

}
