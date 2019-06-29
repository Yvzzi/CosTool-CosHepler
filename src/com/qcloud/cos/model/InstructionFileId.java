package com.qcloud.cos.model;

import com.qcloud.cos.annotation.Immutable;

@Immutable
public class InstructionFileId extends COSObjectId {
    private static final long serialVersionUID = 1L;
    public static final String DEFAULT_INSTRUCTION_FILE_SUFFIX = "instruction";
    public static final String DOT = ".";

    /**
     * Package private to enable the enforcement of naming convention for
     * instruction file.
     * 
     * @param key
     *            key of the instruction file.
     * @param versionId
     *            the version id of an instruction file is expected to be the
     *            same as that of the corresponding (encrypted) COS object
     * 
     * @see COSObjectId#instructionFileId()
     * @see COSObjectId#instructionFileId(String)
     */
    InstructionFileId(String bucket, String key, String versionId) {
        super(bucket, key, versionId);
    }
    
    /**
     * Always throws {@link UnsupportedOperationException} since an instruction
     * file itself cannot further have an instruction file.
     */
    @Override
    public InstructionFileId instructionFileId() {
        throw new UnsupportedOperationException();
    }

    /**
     * Always throws {@link UnsupportedOperationException} since an instruction
     * file itself cannot further have an instruction file.
     */
    @Override
    public InstructionFileId instructionFileId(String suffix) {
        throw new UnsupportedOperationException();
    }
}
