package com.hedera.node.app.hapi.simplefees;

import com.hedera.hapi.node.base.HederaFunctionality;
import com.hedera.node.app.hapi.simplefees.apis.common.AssociateOrDissociate;
import com.hedera.node.app.hapi.simplefees.apis.common.EntityCreate;
import com.hedera.node.app.hapi.simplefees.apis.common.EntityUpdate;
import com.hedera.node.app.hapi.simplefees.apis.common.NoParametersAPI;
import com.hedera.node.app.hapi.simplefees.apis.consensus.HCSSubmit;
import com.hedera.node.app.hapi.simplefees.apis.contract.ContractBasedOnGas;
import com.hedera.node.app.hapi.simplefees.apis.contract.ContractCreate;
import com.hedera.node.app.hapi.simplefees.apis.crypto.CryptoAllowance;
import com.hedera.node.app.hapi.simplefees.apis.crypto.CryptoTransfer;
import com.hedera.node.app.hapi.simplefees.apis.file.FileOperations;
import com.hedera.node.app.hapi.simplefees.apis.token.*;

import java.util.LinkedHashMap;
import java.util.Map;

import static com.hedera.hapi.node.base.HederaFunctionality.*;


public class FeeModelRegistry {
    public static final Map<String, AbstractFeeModel> registry = new LinkedHashMap<>();

    static {
        // Crypto
        registry.put("CryptoCreate", new EntityCreate("Crypto", CRYPTO_CREATE,  "Create a new Account", false));
        registry.put("CryptoTransfer", new CryptoTransfer("Crypto", CRYPTO_TRANSFER));
        registry.put("CryptoUpdate", new EntityUpdate("Crypto", CRYPTO_UPDATE, "Updates an existing account"));
        registry.put("CryptoDelete", new NoParametersAPI("Crypto", CRYPTO_DELETE.name(), "Deletes an existing account"));
        registry.put("CryptoGetAccountRecords", new NoParametersAPI("Crypto", "CryptoGetAccountRecords", "Retrieves records for an account"));
        registry.put("CryptoGetAccountBalance", new NoParametersAPI("Crypto", "CryptoGetAccountBalance", "Retrieves an account’s balance"));
        registry.put("CryptoGetInfo", new NoParametersAPI("Crypto", "CryptoGetInfo", "Retrieves an account’s information"));
        registry.put("CryptoGetStakers", new NoParametersAPI("Crypto", "CryptoGetStakers", "Retrieves the list of proxy stakers for a node"));
        registry.put("CryptoApproveAllowance", new CryptoAllowance( "CryptoApproveAllowance", "Allows a third-party to transfer on behalf of a delegating account (HIP-336)"));
        registry.put("CryptoDeleteAllowance", new CryptoAllowance("CryptoDeleteAllowance", "Deletes non-fungible approved allowances from an owner's account"));

        // HCS
        registry.put("ConsensusCreateTopic", new EntityCreate("Consensus", CONSENSUS_CREATE_TOPIC, "Create a new topic", true));
        registry.put("ConsensusUpdateTopic", new EntityUpdate("Consensus",CONSENSUS_UPDATE_TOPIC, "Update an existing topic"));
        registry.put("ConsensusDeleteTopic", new NoParametersAPI("Consensus", "ConsensusDeleteTopic", "Delete an existing topic"));
        registry.put("ConsensusSubmitMessage", new HCSSubmit());
        registry.put("ConsensusGetTopicInfo", new NoParametersAPI("Consensus", "ConsensusGetTopicInfo", "Retrieve a topic’s metadata"));

        // Token
        registry.put("TokenCreate", new EntityCreate("Token", TOKEN_CREATE, "Create a new token-type", true));
        registry.put("TokenUpdate", new EntityUpdate("Token", TOKEN_UPDATE, "Update an existing token-type"));
//        registry.put("TokenTransfer", new CryptoTransfer("Token", "TokenTransfer"));
        registry.put("TokenDelete", new NoParametersAPI("Token", "TokenDelete", "Delete an existing token"));
        registry.put("TokenMint", new TokenMint());
        registry.put("TokenBurn", new TokenBurn());
        registry.put("TokenPause", new NoParametersAPI("Token", "TokenPause", "Pauses a token"));
        registry.put("TokenUnpause", new NoParametersAPI("Token", "TokenUnpause", "Unpauses a token"));

        registry.put("TokenAirdrop", new CryptoTransfer("Token",TOKEN_AIRDROP));
        registry.put("TokenClaimAirdrop", new TokenAirdropOperations("TokenClaimAirdrop",  "Claim a pending airdrop"));
        registry.put("TokenCancelAirdrop", new TokenAirdropOperations("TokenCancelAirdrop",  "Cancel a pending airdrop"));
        registry.put("TokenReject", new TokenAirdropOperations("TokenReject",  "Reject a token and send back to treasury"));

        registry.put("TokenFeeScheduleUpdate", new NoParametersAPI("Token", "TokenFeeScheduleUpdate", "Updates the custom fee schedule for a token"));
        registry.put("TokenAssociateToAccount", new TokenAssociateDissociate(AssociateOrDissociate.Associate));
        registry.put("TokenDissociateFromAccount", new TokenAssociateDissociate(AssociateOrDissociate.Dissociate));
        registry.put("TokenGrantKycToAccount", new NoParametersAPI("Token", "TokenGrantKycToAccount", "Grant KYC to an account from a particular token"));
        registry.put("TokenRevokeKycFromAccount", new NoParametersAPI("Token", "TokenRevokeKycFromAccount", "Revoke KYC from an account for a particular token"));
        registry.put("TokenFreezeAccount", new NoParametersAPI("Token", "TokenFreezeAccount", "Freeze an account for a particular token"));
        registry.put("TokenUnfreezeAccount", new NoParametersAPI("Token", "TokenUnfreezeAccount", "Unfreeze an account for a particular token"));
        registry.put("TokenAccountWipe", new TokenWipe());
        registry.put("TokenGetInfo", new NoParametersAPI("Token", "TokenGetInfo", "Retrieve a token’s metadata"));
        registry.put("TokenGetNftInfos", new TokenGetNftInfos("GetTokenNftInfos", "Retrieve multiple NFTs' information"));

        // Smart Contracts
        registry.put("ContractCreate", new ContractCreate("ContractCreate", "Create a new Smart Contract", true));
        registry.put("ContractUpdate", new EntityUpdate("Smart Contract", CONTRACT_UPDATE, "Update an existing Smart Contract"));
        registry.put("ContractDelete", new NoParametersAPI("Smart Contract", "ContractDelete", "Delete an existing smart contract"));
        registry.put("ContractCall", new ContractBasedOnGas("ContractCall", "Execute a smart contract call", false));
        registry.put("EthereumTransaction", new ContractBasedOnGas("EthereumTransaction", "Submits a wrapped Ethereum Transaction per HIP-410", false));
        registry.put("ContractGetInfo", new NoParametersAPI("Smart Contract", "ContractGetInfo", "Retrieve a smart contract’s metadata"));
        registry.put("ContractCallLocal", new NoParametersAPI("Smart Contract", "ContractCallLocal", "Execute a smart contract call on a single node"));
        registry.put("ContractGetBytecode", new NoParametersAPI("Smart Contract", "ContractGetBytecode", "Retrieve a smart contract’s bytecode"));


        // File
        registry.put("FileCreate", new FileOperations(FILE_CREATE, "Create a new file"));
        registry.put("FileUpdate", new FileOperations(FILE_UPDATE, "Update an existing file"));
        registry.put("FileDelete", new NoParametersAPI("File", FILE_DELETE.name(), "Delete an existing file"));
        registry.put("FileAppend", new FileOperations(FILE_APPEND, "Append to an existing file"));
        registry.put("FileGetContents", new NoParametersAPI("File",FILE_GET_CONTENTS.name(), "Retrieve the contents of a file"));
        registry.put("FileGetInfo", new NoParametersAPI("File", FILE_GET_INFO.name(), "Retrieve a file’s metadata"));


        // Miscellaneous
        registry.put("ScheduleCreate", new EntityCreate("Miscellaneous", SCHEDULE_CREATE, "Create a new scheduled transaction", false));
        registry.put("ScheduleSign", new NoParametersAPI("Miscellaneous", "ScheduleSign", "Add a signature to a scheduled transaction"));
        registry.put("ScheduleDelete", new NoParametersAPI("Miscellaneous", "ScheduleDelete", "Delete a scheduled transaction"));
        registry.put("ScheduleGetInfo", new NoParametersAPI("Miscellaneous", "ScheduleGetInfo", "Retrieve information about a scheduled transaction"));

        registry.put("GetVersionInfo", new NoParametersAPI("Miscellaneous", "GetVersionInfo", "Retrieve the current version of the network"));
        registry.put("TransactionGetReceipt", new NoParametersAPI("Miscellaneous", "TransactionGetReceipt", "Retrieve a transaction’s receipt"));
        registry.put("TransactionGetRecord", new NoParametersAPI("Miscellaneous", "TransactionGetRecord", "Retrieve a transaction’s record"));
        registry.put("SystemDelete", new NoParametersAPI("Miscellaneous", "SystemDelete", "System delete an existing file"));
        registry.put("SystemUndelete", new NoParametersAPI("Miscellaneous", "SystemUndelete", "System undelete an existing file"));
        registry.put("PrngTransaction", new NoParametersAPI("Miscellaneous", "PrngTransaction", "Generate a pseudorandom number"));
        registry.put("CreateNode", new NoParametersAPI("Miscellaneous", "CreateNode", "Add a new node to the address book"));
        registry.put("DeleteNode", new NoParametersAPI("Miscellaneous", "DeleteNode", "Delete a node from the address book"));
        registry.put("UpdateNode", new NoParametersAPI("Miscellaneous", "UpdateNode", "Modify node attributes"));
        registry.put("BatchTransaction", new NoParametersAPI("Miscellaneous", "BatchTransaction", "Submit outer transaction containing a batch"));
    }
}
