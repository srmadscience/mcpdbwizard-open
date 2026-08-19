package com.mcpdbwizard.util.files;

import com.mcpdbwizard.util.AbstractBaseStoredFile;

/**
 * Copyright 2003-2026 ATB Consultancy Services Ltd
 * (formerly Orinda Software Ltd, Dublin, Ireland)
 */
public class Lurkandbuild2Bat extends AbstractBaseStoredFile {
    public Lurkandbuild2Bat() {
        super();

        this.name = "lurkandbuild2.bat";
        this.descr = "Create Wise Images";
        this.os = "Windows";

        String[] lines = {
                "c:"
                , "cd \\DR"
                , "cd Work"
                , "cd CodeSpooks"
                , "cd WiseImages"
                , "del MCPDBWizard*.exe"
                , "cd .."
                , ""
                , "del go.txt"
                , ""
                , ":start"
                , ""
                , "\"C:\\Program Files (x86)\\PuTTY\\psftp.exe\" -pw idontknow -bc -b Wise\\sftpcommands_check.txt  spooky@203.0.113.10"
                , ""
                , "if exist go.txt goto :build"
                , ""
                , "timeout /t 30"
                , ""
                , "goto :start"
                , ""
                , ""
                , ":build"
                , "del PublishLatest.tar"
                , "\"C:\\Program Files (x86)\\PuTTY\\psftp.exe\" -pw idontknow -bc -b Wise\\sftpcommands_get.txt  spooky@203.0.113.10"
                , "rmdir /s /q Publish"
                , "\"C:\\Program Files\\7-Zip\\7z.exe\" x -y PublishLatest.tar"
                , ""
                , ""
                , "timeout /t 30"
                , ""
                , ""
                , "call x:\\makewiseimages.bat"
                , ""
                , ":part2"
                , ""
                , ""
                , ""
                , "c:"
                , "cd \\DR"
                , "cd Work"
                , "cd CodeSpooks"
                , "cd WiseImages"
                , ""
                , "dir *.exe"
                , ""
                , "\"C:\\Program Files (x86)\\PuTTY\\psftp.exe\" -pw idontknow -bc -b ..\\Wise\\sftpcommands.txt  spooky@203.0.113.10"
                , ""
                , "timeout /t 180"
                , ""
                , ":backup"
                , ""
                , ""
                , ""
                , "c:"
                , "cd \\DR"
                , "cd Downloads"
                , "cd CodeSpooksBackups"
                , ""
                , ""
                , "del *.Z"
                , ""
                , "del go2.txt"
                , ""
                , ":start2"
                , ""
                , ""
                , ""
                , "\"C:\\Program Files (x86)\\PuTTY\\psftp.exe\" -pw idontknow -bc -b C:\\DR\\Work\\CodeSpooks\\Wise\\sftpcommands_check2.txt  spooky@203.0.113.10"
                , ""
                , "if exist go2.txt goto :build2"
                , ""
                , "timeout /t 30"
                , ""
                , "goto :start2"
                , ""
                , ""
                , ":build2"
                , ""
                , "c:"
                , "cd \\DR"
                , "cd Downloads"
                , "cd CodeSpooksBackups"
                , ""
                , "del *.Z"
                , ""
                , "\"C:\\Program Files (x86)\\PuTTY\\psftp.exe\" -pw idontknow -bc -b C:\\DR\\Work\\CodeSpooks\\Wise\\sftpcommands_getbackup.txt  spooky@203.0.113.10"
                , ""
                , "mkdir G:\\CodeSpooksBackups"
                , ""
                , "copy *.Z  G:\\CodeSpooksBackups"
                , "pause"
        };


        this.contents = lines;
    }
}
