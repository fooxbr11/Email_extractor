package com.emailattachsender.emailattach;

import javax.mail.*;
import javax.mail.internet.MimeBodyPart;
import javax.mail.search.SearchTerm;
import javax.mail.search.SubjectTerm;
import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.logging.*;

public class EmailAttachmentDownloader {

    private static final Logger logger = Logger.getLogger(EmailAttachmentDownloader.class.getName());

    static {
        setupLogger();
    }

    public void downloadAttachments(Properties config) throws Exception {
        // Configuração do servidor de e-mail (IMAP)
        String host = config.getProperty("host", "imap.gmail.com"); // Servidor IMAP
        String username = config.getProperty("username"); // E-mail de autenticação
        String password = config.getProperty("password"); // Senha de app
        String inbox = config.getProperty("inbox", "INBOX"); // Caixa de entrada
        String currentDir = System.getProperty("user.dir");
        String defaultDestDir = currentDir + File.separator + "pdfsaver";
        String destDir = config.getProperty("destDir", defaultDestDir); // Diretório de armazenamento

        // Verificar se o diretório de destino existe e criar se não existir
        File directory = new File(destDir);
        if (!directory.exists()) {
            directory.mkdirs();
            logger.info("Diretório de destino criado: " + destDir);
        }
        if (!directory.isDirectory()) {
            logger.severe("Diretório de destino inválido.");
            throw new IOException("Diretório de destino inválido.");
        }

        // Propriedades do servidor de e-mail
        Properties properties = new Properties();
        properties.put("mail.store.protocol", "imaps");

        try {
            // Criar uma sessão de e-mail
            Session emailSession = Session.getDefaultInstance(properties);
            emailSession.setDebug(true); // Ativar logs de depuração para a sessão
            logger.info("Sessão de e-mail criada.");

            // Criar o objeto de store e conectar-se ao servidor de e-mail
            Store store = emailSession.getStore("imaps");
            logger.info("Conectando ao servidor de e-mail...");
            store.connect(host, username, password);
            logger.info("Conectado ao servidor de e-mail.");

            // Conectar-se à pasta especificada
            Folder emailFolder = store.getFolder(inbox);
            emailFolder.open(Folder.READ_ONLY);
            logger.info("Conectado à pasta " + inbox + ".");

            // Buscar mensagens com o assunto especificado
            SearchTerm searchTerm = new SubjectTerm("[GRAVIA] :: Novo Pedido de Compras");
            Message[] messages = emailFolder.search(searchTerm);
            logger.info("Total de mensagens encontradas: " + messages.length);

            int downloadCount = 0;

            Pattern pattern = Pattern.compile("#(\\d+)");
            for (Message message : messages) {
                // Processar os anexos
                logger.info("Processando mensagem: " + message.getSubject());

                // Verifica se a mensagem é do tipo multipart
                if (message.isMimeType("multipart/*")) {
                    Multipart multipart = (Multipart) message.getContent();
                    for (int i = 0; i < multipart.getCount(); i++) {
                        MimeBodyPart part = (MimeBodyPart) multipart.getBodyPart(i);
                        if (Part.ATTACHMENT.equalsIgnoreCase(part.getDisposition()) && part.getFileName().endsWith(".pdf")) {
                            processAttachment(message, part, destDir, pattern);
                            downloadCount++;
                        }
                    }
                } else if (message.isMimeType("APPLICATION/PDF")) {
                    // Caso seja um PDF anexado diretamente
                    if (message.getFileName() != null && message.getFileName().endsWith(".pdf")) {
                        MimeBodyPart part = new MimeBodyPart();
                        part.setDataHandler(message.getDataHandler());
                        part.setFileName(message.getFileName());
                        processAttachment(message, part, destDir, pattern);
                        downloadCount++;
                    } else {
                        logger.info("Mensagem ignorada. Tipo MIME: " + message.getContentType());
                    }
                } else {
                    logger.info("Mensagem ignorada. Tipo MIME: " + message.getContentType());
                }
            }

            // Fechar conexões
            emailFolder.close(false);
            store.close();
            logger.info("Conexão fechada.");

            if (downloadCount == 0) {
                logger.info("Nenhum novo arquivo encontrado para download.");
                throw new Exception("Nenhum novo arquivo encontrado para download.");
            }

        } catch (AuthenticationFailedException e) {
            logger.severe("Erro de autenticação: " + e.getMessage());
            throw new Exception("Erro de autenticação: verifique seu e-mail e senha.", e);
        } catch (MessagingException | IOException e) {
            logger.severe("Erro ao processar e-mails: " + e.getMessage());
            throw new Exception("Erro ao processar e-mails: " + e.getMessage(), e);
        }
    }

    private void processAttachment(Message message, MimeBodyPart part, String destDir, Pattern pattern) throws MessagingException, IOException {
        // Extrair ID do arquivo
        Matcher matcher = pattern.matcher(message.getSubject());
        if (matcher.find()) {
            String fileId = matcher.group(1);
            logger.info("Verificando se o arquivo já foi baixado. FileId: " + fileId);
            if (!DatabaseUtils.isFileAlreadyDownloaded(fileId)) {
                // Salvar o anexo na pasta
                logger.info("Salvando anexo: " + part.getFileName());
                if (saveAttachment(part, destDir)) {
                    // Registrar no banco de dados
                    logger.info("Registrando anexo no banco de dados. FileId: " + fileId);
                    DatabaseUtils.logDownloadedFile(fileId, LocalDateTime.now());
                } else {
                    logger.warning("Falha ao salvar anexo: " + part.getFileName());
                }
            } else {
                logger.info("Arquivo já baixado anteriormente: " + part.getFileName());
            }
        } else {
            logger.warning("ID do arquivo não encontrado no assunto da mensagem: " + message.getSubject());
        }
    }

    private boolean saveAttachment(MimeBodyPart part, String destDir) throws IOException, MessagingException {
        String fileName = part.getFileName().replaceAll("[\\\\/:*?\"<>|]", "_"); // Sanitizar o nome do arquivo

        File file = new File(destDir + File.separator + fileName);

        // Verificar se o arquivo já existe
        if (file.exists()) {
            logger.info("Arquivo já existe: " + file.getAbsolutePath());
            return false; // Não baixou novo arquivo
        } else {
            part.saveFile(file);
            logger.info("Salvo: " + file.getAbsolutePath());
            return true; // Baixou novo arquivo
        }
    }

    private static void setupLogger() {
        try {
            // Especificar um caminho absoluto ou relativo para o arquivo de log
            String logDir = "logs";
            File logDirFile = new File(logDir);
            if (!logDirFile.exists()) {
                logDirFile.mkdirs(); // Cria o diretório se não existir
            }
            FileHandler fh = new FileHandler(logDir + "/EmailAttachmentDownloader.log", true);
            fh.setFormatter(new CustomFormatter());
            logger.addHandler(fh);
        } catch (IOException e) {
            e.printStackTrace();
            logger.severe("Erro ao configurar logger: " + e.getMessage());
        }
    }

    private static class CustomFormatter extends Formatter {
        @Override
        public String format(LogRecord record) {
            StringBuilder builder = new StringBuilder();
            builder.append(new java.util.Date(record.getMillis()))
                    .append(" [")
                    .append(record.getLevel()).append("] - ")
                    .append(formatMessage(record))
                    .append(System.lineSeparator());
            return builder.toString();
        }
    }
}
