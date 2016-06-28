package com.neurotec.testing;

import com.neurotec.biometrics.*;
import com.neurotec.biometrics.client.NBiometricClient;
import com.neurotec.images.NImage;
import com.neurotec.images.NImageFormat;
import com.neurotec.io.NBuffer;
import com.neurotec.licensing.NLicense;


import java.io.IOException;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.UUID;

public class MysqlConnection {
    private NBiometricClient client;

    private String baseDir = "/Users/rkasigi/Workspace/neurotec_tes" + FILE_SEPARATOR;
    private String odbcConnectionString = "DSN=mysql_dsn;UID=root;PWD=123;DATABASE=neurotec_tes;CHARSET=utf8;BIG_PACKETS=8";
    private String odbcTableName = "bio_subjects";

    public static final String FILE_SEPARATOR = System.getProperty("file.separator");
    public static final String PATH_SEPARATOR = System.getProperty("path.separator");
    public static final String LINE_SEPARATOR = System.getProperty("line.separator");



    public static void main(String[] args) throws IOException {


        final String components = "Biometrics.FingerExtraction,Biometrics.FingerMatching,Images.WSQ";

        if (!NLicense.obtainComponents("/local", 5000, components)) {
            System.err.format("Could not obtain licenses for components: %s%n", components);
            System.exit(-1);
        }



        MysqlConnection app = new MysqlConnection();

        app.run();

    }

    public void run() throws IOException {
        connectDatabase();
        enroll();

    }


    private void connectDatabase() {
        System.out.println("Connecting database....");

        client = new NBiometricClient();
        client.setDatabaseConnectionToOdbc(odbcConnectionString, odbcTableName);
        client.initialize();

    }


    private void enroll() throws IOException {
        System.out.println("Do Enrollment....");

        String randomID = UUID.randomUUID().toString();

        NSubject nSubject = new NSubject();
        nSubject.setId(randomID);
        nSubject.setGender(NGender.MALE);

        List<NFPosition> fingersName = new ArrayList<>();
        fingersName.add(NFPosition.LEFT_LITTLE_FINGER);
        fingersName.add(NFPosition.LEFT_RING_FINGER);
        fingersName.add(NFPosition.LEFT_MIDDLE_FINGER);
        fingersName.add(NFPosition.LEFT_INDEX_FINGER);
        fingersName.add(NFPosition.LEFT_THUMB);

        fingersName.add(NFPosition.RIGHT_LITTLE_FINGER);
        fingersName.add(NFPosition.RIGHT_RING_FINGER);
        fingersName.add(NFPosition.RIGHT_MIDDLE_FINGER);
        fingersName.add(NFPosition.RIGHT_INDEX_FINGER);
        fingersName.add(NFPosition.RIGHT_THUMB);



        for(NFPosition fingerPosition : fingersName) {

            String imageFile = String.format("%sfingers%s%s.jpg", baseDir, FILE_SEPARATOR, fingerPosition);
            NImage nImage = NImage.fromFile(imageFile);

            /**
             * force setup image resolution
             */
            nImage.setHorzResolution(500);
            nImage.setVertResolution(500);
            NBuffer nImageBuffer = nImage.save(NImageFormat.getWSQ());
            nImage = NImage.fromMemory(nImageBuffer);
            nImageBuffer.dispose();

            NFinger nFinger = new NFinger();
            nFinger.setImage(nImage);
            nFinger.setPosition(fingerPosition);

            nSubject.getFingers().add(nFinger);

        }

        client.reset();
        client.setFingersTemplateSize(NTemplateSize.LARGE);
        client.setFingersQualityThreshold((byte) 0);


        NBiometricTask task = client.createTask(EnumSet.of(NBiometricOperation.ENROLL), nSubject);
//		NBiometricTask task = client.createTask(EnumSet.of(NBiometricOperation.ENROLL_WITH_DUPLICATE_CHECK), nSubject);
        client.performTask(task);


        System.out.println("Enroll Status: " + task.getStatus());

        if(!task.getStatus().equals(NBiometricStatus.OK)) {
            System.out.println("Got error when enroll: ");

            if(task.getError() != null)
            {
                task.getError().printStackTrace();
            }
        }

    }
}
