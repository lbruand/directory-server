/*
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *  
 *    http://www.apache.org/licenses/LICENSE-2.0
 *  
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License. 
 *  
 */
package org.apache.directory.shared.kerberos.codec.tgsRep.actions;


import org.apache.directory.shared.asn1.ber.Asn1Container;
import org.apache.directory.shared.asn1.ber.Asn1Decoder;
import org.apache.directory.shared.asn1.ber.grammar.GrammarAction;
import org.apache.directory.shared.asn1.ber.tlv.TLV;
import org.apache.directory.shared.asn1.codec.DecoderException;
import org.apache.directory.shared.i18n.I18n;
import org.apache.directory.shared.kerberos.codec.KerberosMessageGrammar;
import org.apache.directory.shared.kerberos.codec.kdcRep.KdcRepContainer;
import org.apache.directory.shared.kerberos.codec.tgsRep.TgsRepContainer;
import org.apache.directory.shared.kerberos.messages.TgsRep;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * The action used to add a KDC-REP object
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class StoreKdcRep extends GrammarAction
{
    /** The logger */
    private static final Logger LOG = LoggerFactory.getLogger( KerberosMessageGrammar.class );

    /** Speedup for logs */
    private static final boolean IS_DEBUG = LOG.isDebugEnabled();


    /**
     * Instantiates a new KDC-REP action.
     */
    public StoreKdcRep()
    {
        super( "Add an KDC-REP instance" );
    }


    /**
     * {@inheritDoc}
     */
    public void action( Asn1Container container ) throws DecoderException
    {
        TgsRepContainer tgsRepContainer = ( TgsRepContainer ) container;

        TLV tlv = tgsRepContainer.getCurrentTLV();

        // The Length should not be null
        if ( tlv.getLength() == 0 )
        {
            LOG.error( I18n.err( I18n.ERR_04066 ) );

            // This will generate a PROTOCOL_ERROR
            throw new DecoderException( I18n.err( I18n.ERR_04067 ) );
        }
        
        // Now, let's decode the KDC-REP
        Asn1Decoder kdcRepDecoder = new Asn1Decoder();
        
        KdcRepContainer kdcRepContainer = new KdcRepContainer();
        kdcRepContainer.setStream( container.getStream() );
        
        // Store the created TGS-REP object into the KDC-REP container
        TgsRep tgsRep = new TgsRep();
        kdcRepContainer.setKdcRep( tgsRep );
        
        // Decode the KDC_REP PDU
        try
        {
            kdcRepDecoder.decode( container.getStream(), kdcRepContainer );
        }
        catch ( DecoderException de )
        {
            throw de;
        }
        
        // Update the expected length for the current TLV
        tlv.setExpectedLength( tlv.getExpectedLength() - tlv.getLength() );

        // Update the parent
        container.updateParent();
        
        tgsRepContainer.setTgsRep( tgsRep );

        if ( IS_DEBUG )
        {
            LOG.debug( "TGS-REP : {}", tgsRep );
        }
        
        container.setGrammarEndAllowed( true );
    }
}
