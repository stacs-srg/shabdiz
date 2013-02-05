/*
 * Copyright (C) 2013 Qraqrbox.com. All Rights Reserved.
 *
 * This file is part of the Qraqrbox project.
 *
 * This project part of the Sottish Informatics and Computer Science Alliance
 * (SICSA)[1] Smart Tourism Initiative [2] funded by the SFC [3]which brings together
 * university informatics and computing science research from across SICSA,
 * tourism organisations, and industry to address some of the key challenges
 * in the sector. The purpose of the Smart Tourism project is to deliver
 * innovative approaches and technology solutions to enhance the Scottish
 * Tourism sectors technology base.
 *
 * For more information, please contact <enquiries@qraqrbox.com>.
 *
 * [1] <http://www.sicsa.ac.uk>
 * [2] <http://www.smarttourism.org>
 * [3] <http://www.sfc.ac.uk>
 */

package uk.ac.standrews.cs.shabdiz.impl;

import org.apache.commons.io.IOUtils;

import java.io.IOException;

public final class ProcessUtil {

    private static final int NORMAL_TERMINATION = 0;

    private ProcessUtil() {
    }

    public static String waitForAndReadOutput(Process process) throws IOException, InterruptedException {
        int exit_value = process.waitFor();
        try {
            switch (exit_value) {
                case NORMAL_TERMINATION:
                    return IOUtils.toString(process.getInputStream());
                default:
                    throw new IOException();
            }
        } finally {
            process.destroy();
        }
    }
}
