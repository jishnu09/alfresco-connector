/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.alfresco.connector;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.chemistry.opencmis.client.api.Document;
import org.apache.chemistry.opencmis.client.api.Folder;
import org.apache.chemistry.opencmis.client.api.Session;
import org.apache.chemistry.opencmis.client.api.SessionFactory;
import org.apache.chemistry.opencmis.client.runtime.SessionFactoryImpl;
import org.apache.chemistry.opencmis.commons.PropertyIds;
import org.apache.chemistry.opencmis.commons.SessionParameter;
import org.apache.chemistry.opencmis.commons.data.ContentStream;
import org.apache.chemistry.opencmis.commons.enums.BindingType;
import org.apache.chemistry.opencmis.commons.enums.VersioningState;
import org.apache.chemistry.opencmis.commons.exceptions.CmisContentAlreadyExistsException;
import org.apache.chemistry.opencmis.commons.exceptions.CmisObjectNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author SwarnenduS
 */
public class AlfrescoConnector {

    private final Logger log = LoggerFactory.getLogger(AlfrescoConnector.class);

    private String alfrescoUser = null;
    private String alfrescoPassword = null;
    private String alfrescoUrl = null;
    private Folder tempFolder = null;
    private String fullPath = null;

    public AlfrescoConnector(String alfrescoUser, String alfrescoPassword, String alfrescoUrl) {
        this.alfrescoUser = alfrescoUser;
        this.alfrescoPassword = alfrescoPassword;
        this.alfrescoUrl = alfrescoUrl;
        log.info("Constructor initialized..........");
    }

    public static void main(String arr[]) {
        AlfrescoConnector obj = new AlfrescoConnector(arr[0], arr[1], arr[2]);
        switch(arr[3].toUpperCase()){
            case "UPLOAD" : 
                break;
            case "DOWNLOAD":
                obj.downloadFileInAlfresco(arr[4]);
                break;
            default:
                break;
        }
    }

    public String uploadFileInAlfresco(String basePath, String path, File file, String mimeType) {
        String send = null;
        Session session = null;
        try {

            log.info("Base path: " + basePath);
            log.info("File path: " + path);
            log.info("File: " + file.getName());
            log.info("Mime type: " + mimeType);

            log.info("Starting file upload..........");

            session = createAlfrescoSession();
            Folder folder = createFolderBypath(session, path, basePath);
            Document createFileInAlfresco = createFileInAlfresco(session, folder, file, mimeType);
            log.info("File having id--->" + createFileInAlfresco.getId());
            send = createFileInAlfresco.getId();
            file.delete();
            log.info("Finished uploading");
        } catch (Exception e) {
            log.error("", e);
            send = null;
        }
        session = null;
        return send;
    }

    public Document downloadFileInAlfresco(String id) {
        Document send = null;
        Session session = null;
        try {

            log.info("File id: " + id);
            log.info("Starting file download..........");
            session = createAlfrescoSession();
            send = getDocument(session, id);

            log.info("Finished downloading");
            log.info("File Name: " + send.getName());
        } catch (Exception e) {
            log.error("", e);
            send = null;
        }
        session = null;
        return send;
    }

    private Session createAlfrescoSession() {
        Session session = null;
        try {
            log.info("Creating session........");
            SessionFactory sessionFactory = SessionFactoryImpl.newInstance();
            Map parameter = new HashMap();
//            parameter.put(SessionParameter.USER, "admin");
//            parameter.put(SessionParameter.PASSWORD, "admin");
//            parameter.put(SessionParameter.BROWSER_URL, "http://172.20.24.133:18080/alfresco/api/-default-/public/cmis/versions/1.1/browser/");
            parameter.put(SessionParameter.USER, alfrescoUser);
            parameter.put(SessionParameter.PASSWORD, alfrescoPassword);
            parameter.put(SessionParameter.BROWSER_URL, alfrescoUrl);
            parameter.put(SessionParameter.BINDING_TYPE, BindingType.BROWSER.value());
            //parameter.put(SessionParameter.BINDING_TYPE, BindingType.ATOMPUB.value());
            parameter.put(SessionParameter.REPOSITORY_ID, "-default-");
            session = sessionFactory.createSession(parameter);
            log.info("Session created........");
        } catch (Exception e) {
            log.error("Problem in creating alfresco session....", e);
        }
        return session;
    }

    private synchronized void createFolder(Session session, Folder folder, String name, String path, String basePath) throws CmisContentAlreadyExistsException {
        log.info("Creating '" + name + "' in the " + folder.getName() + " folder");
        Map<String, String> newFolderProps = new HashMap<String, String>();
        newFolderProps.put(PropertyIds.OBJECT_TYPE_ID, "cmis:folder");
        newFolderProps.put(PropertyIds.NAME, name);

        Folder newFolder = folder.createFolder(newFolderProps);
        createFolderBypath(session, path, basePath);
    }

    private synchronized Folder createFolderBypath(Session session, String path, String basePath) {
        Folder folder = null;
        String name = null;
        //String newPath = "MCUBE/Selenium/Screen Shots/" + path;
        String newPath = basePath + path;
        String tempPath = "";

        try {
            //log.info("Root Folder: " + session.getRootFolder().getName());
            log.info("newPath: " + newPath);

//            for (Repository r : repositories) {
//                //log.info("Found repository: " + r.getName());
//            }
            //log.info("Root Folder: " + session.getRootFolder().getName());
            String[] pathSplit = newPath.split("/");
            //log.info("pathSplit--->" + Arrays.toString(pathSplit));
            if (null == tempFolder) {
                //tempFolder = (Folder) session.getObjectByPath("/MCUBE/Selenium/Screen Shots/");
                tempFolder = (Folder) session.getObjectByPath(basePath);
            }

            for (int i = 0; i < pathSplit.length; i++) {
                tempPath = tempPath + "/" + pathSplit[i];
                //log.info("tempPath--->" + tempPath);
                name = pathSplit[i];
                Folder objectByPath = (Folder) session.getObjectByPath(tempPath);
                tempFolder = objectByPath;
                fullPath = tempPath;
            }
            //log.info("newPath---->" + newPath);
//            session.clear();
//            session=sessionFactory.createSession(parameter);

            //log.info("Folder name--->" + folder.getName());
        } catch (CmisObjectNotFoundException e) {
            log.error("", e.getMessage());
            if (tempFolder != null) {
                fullPath = tempPath;
                tempPath = "";
                try {
                    createFolder(session, tempFolder, name, path, basePath);
                } catch (CmisContentAlreadyExistsException ex) {
                    log.error("", ex);
                    createFolderBypath(session, path, basePath);
                }

            }
        } catch (Exception e) {
            log.error("", e);

        }
        if (session != null) {
//            log.info("newPath-->" + newPath);
//            log.info("tempPath-->" + tempPath);
//            log.info("fullPath-->" + fullPath);
//            log.info("Calling");
            if (null != fullPath) {
                folder = (Folder) session.getObjectByPath(fullPath);
            }

            //log.info("Called");
        }

        //folder = (Folder) session.getObjectByPath(tempPath);
        if (null == folder) {
            return null;
        } else {
            //log.info("Folder name--1-->" + folder.getName());
            return folder;
        }

    }

    private synchronized Document createFileInAlfresco(Session session, Folder folder, File file, String mimetype) {
        Document doc = null;
        //String send = null;
        ByteArrayOutputStream ous = null;
        InputStream ios = null;
        ByteArrayInputStream input = null;
        log.info("Creating file  " + file.getName() + " in folder " + folder.getName());
        try {

            //String mimetype = "image/jpeg; charset=UTF-8";
            //byte[] buf = file.getBytes("UTF-8");
            ous = new ByteArrayOutputStream();
            byte[] buffer = new byte[4096];
            ios = new FileInputStream(file);
            int read = 0;
            while ((read = ios.read(buffer)) != -1) {
                ous.write(buffer, 0, read);
            }
            input = new ByteArrayInputStream(ous.toByteArray());

            ContentStream contentStream = session.getObjectFactory().createContentStream(file.getName(), ous.toByteArray().length, mimetype, input);
            Map<String, Object> properties = new HashMap<>();
            properties.put(PropertyIds.OBJECT_TYPE_ID, "cmis:document");
            properties.put(PropertyIds.NAME, file.getName());

            doc = folder.createDocument(properties, contentStream, VersioningState.MAJOR);

        } catch (Exception e) {
            log.error("", e);
        } finally {
            if (null != input) {
                try {
                    input.close();
                } catch (IOException ex) {
                    log.error("", ex);
                }
            }
            if (null != ous) {
                try {
                    ous.close();
                } catch (IOException ex) {
                    log.error("", ex);
                }
            }
            if (null != ios) {
                try {
                    ios.close();
                } catch (IOException ex) {
                    log.error("", ex);
                }

            }
            if (null != file) {
                file.delete();
            }
        }
//        if (doc != null) {
//            send = doc;
//        }
        log.info("send--->" + doc);
        return doc;
    }

    private Document getDocument(Session session, String objectId) {
        InputStream inputStream = null;
        OutputStream outputStream = null;
        Document doc = null;
        try {

            doc = (Document) session.getObject(objectId);

            ContentStream contentStream = doc.getContentStream();
            if (null != contentStream) {
                List<Folder> parents = doc.getParents();
                log.info(parents.get(parents.size() - 1).getName());
//                for (Folder folder : parents) {
//                    log.info(folder.getName());
//                }
                log.info("Mime--->" + contentStream.getMimeType());
                if (contentStream.getMimeType().equalsIgnoreCase("image/jpeg")) {
                    outputStream = new FileOutputStream(new File("/Selenium/temp/Downloads/images/" + doc.getName()));
                    inputStream = contentStream.getStream();
                    int read = 0;
                    byte[] bytes = new byte[1024];

                    while ((read = inputStream.read(bytes)) != -1) {
                        outputStream.write(bytes, 0, read);
                    }
                } else if (contentStream.getMimeType().equalsIgnoreCase("application/pdf")) {
                    outputStream = new FileOutputStream(new File("/Selenium/temp/Downloads/pdf/" + doc.getName()));
                    inputStream = contentStream.getStream();
                    int read = 0;
                    byte[] bytes = new byte[1024];

                    while ((read = inputStream.read(bytes)) != -1) {
                        outputStream.write(bytes, 0, read);
                    }
                }
            }

        } catch (Exception e) {
            log.error("", e);
        } finally {
            if (null != outputStream) {
                try {
                    outputStream.close();
                } catch (IOException ex) {
                    log.error("", ex);
                }
            }
            if (null != inputStream) {
                try {
                    inputStream.close();
                } catch (IOException ex) {
                    log.error("", ex);
                }
            }
        }
        return doc;
    }
}
